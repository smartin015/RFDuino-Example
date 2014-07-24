/* RFDuino BLE Echo App
 * Paul Lutz
 * Scott Martin
 * 7/2014
 */

package com.example.bleecho;

import java.io.UnsupportedEncodingException;
import java.util.List;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {
	private final static String TAG = MainActivity.class.getSimpleName();
	private final static String ERROR_TAG = "ERROR";

    private BluetoothAdapter bluetoothAdapter; 
    private BluetoothManager bluetoothManager;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic sendCharacteristic = null;
    private BluetoothGattCharacteristic receiveCharacteristic = null;
    
    private String bluetoothDeviceAddress = "";
    private static final String DEVICE_NAME = "RFduino";
    
    private String connectionState = STATE_DISCONNECTED;
    private static final String STATE_DISCONNECTED = "Disconnected";
    private static final String STATE_CONNECTING = "Connecting...";
    private static final String STATE_CONNECTED = "Connected";

    private static final String UPDATE_STATUS_INTENT = "ble.echo.update.STATUS";
    private static final String UPDATE_RESPONSE_INTENT = "ble.echo.update.RESPONSE";
    
    private static final String RECEIVE_CHARACTERISTIC_UUID = "2222";
    private static final String SEND_CHARACTERISTIC_UUID = "2221";
    
    private boolean isScanning;
    private Handler scanTimeoutHandler;
    
    private static final int REQUEST_ENABLE_BT = 1;
    
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private String stringToDisplay = "";
    
    Button btnRefreshConnection;
    Button btnSend;
    EditText editToSend;
    TextView textResponse;
    TextView textConnectionStatus;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        Log.w(TAG, "Setting up...");
        
        scanTimeoutHandler = new Handler();

        // This filter will update the status text view
        IntentFilter statusFilter = new IntentFilter();
        statusFilter.addAction(UPDATE_STATUS_INTENT);
        registerReceiver(statusReceiver, statusFilter);

        // This filter will update the response text view
        IntentFilter responseFilter = new IntentFilter();
        responseFilter.addAction(UPDATE_RESPONSE_INTENT);
        registerReceiver(responseReceiver, responseFilter);
        
        // Initialize UI elements
		btnRefreshConnection = (Button) findViewById(R.id.btnRefreshConnection);
	    btnSend = (Button) findViewById(R.id.btnSend);
	    editToSend = (EditText) findViewById(R.id.editToSend);
	    textResponse = (TextView) findViewById(R.id.textResponse);
	    textConnectionStatus = (TextView) findViewById(R.id.textConnectionStatus);
	    
		// Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
    		Log.w(ERROR_TAG,"BLE not supported");
            finish();
        }

		Log.w(TAG,"Initializing Bluetooth Manager");
        bluetoothManager =
            (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        
		Log.w(TAG,"Initializing Bluetooth Adapter");
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
    		Log.w(ERROR_TAG,"Bluetooth not supported");
            finish();
        }
        
        btnRefreshConnection.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	setScanning(true);
            }
        });
        
        btnSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	if(sendCharacteristic == null || receiveCharacteristic == null) {
            		Log.w(ERROR_TAG,"Invalid characteristic");
            		return;
            	}
            	
            	if(connectionState.equals(STATE_DISCONNECTED)) {
            		Log.w(ERROR_TAG,"You're not connected!");
            		return;
            	}
            
            	writeDataToCharacteristic(receiveCharacteristic,editToSend.getText().toString().getBytes());
            	readDataFromCharacteristic(sendCharacteristic);
            }
        });
	}
	
	private void setScanning(final boolean enable) {
		if(isScanning) {
    		Log.w(TAG,"Already Scanning");
    		return;
    	} 
		if(connectionState.equals(STATE_CONNECTED)) {
    		Log.w(TAG,"Already connected");
    		return;
    	}
		
        if (enable) {
            // Stops scanning after a pre-defined scan period.
        	scanTimeoutHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isScanning = false;
                    bluetoothAdapter.stopLeScan(scanCallback);
                }
            }, SCAN_PERIOD);

            isScanning = true;
            bluetoothAdapter.startLeScan(scanCallback);
        } else {
            isScanning = false;
            bluetoothAdapter.stopLeScan(scanCallback);
        }
    }
	
    private BluetoothAdapter.LeScanCallback scanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
        	Log.w(TAG, "Found device: " + device.getName());
        	if(device.getName().equals(DEVICE_NAME))
        	{ 
            	Log.w(TAG,device.getAddress());
            	connect(device);
        	}
        }
    };
    
    public boolean connect(BluetoothDevice device) {
        if (bluetoothAdapter == null || device == null) {
            Log.w(ERROR_TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (device.getAddress().equals(bluetoothDeviceAddress) && bluetoothGatt != null) {
            Log.d(TAG, "Using existing bluetoothGatt for connection.");
            if (bluetoothGatt.connect()) {
                connectionState = STATE_CONNECTING;
                updateStatusIntent();
                return true;
            } else {
                return false;
            }
        }

        Log.d(TAG, "Creating new connection");
        bluetoothGatt = device.connectGatt(this, false, mGattCallback);
        bluetoothDeviceAddress = device.getAddress();
        
        connectionState = STATE_CONNECTING;
        updateStatusIntent();
        return true;
    }
    
    private BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	if (intent.getAction().equals(UPDATE_STATUS_INTENT)) {
                Log.w(TAG,"Status Changed: "+connectionState);
            	textConnectionStatus.setText(connectionState);
            }
        }
    };
    
    private BroadcastReceiver responseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	if (intent.getAction().equals(UPDATE_RESPONSE_INTENT)) {
                Log.w(TAG,"Response Received: "+stringToDisplay);
            	textResponse.setText(stringToDisplay);
            }
        }
    };
    
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectionState = STATE_CONNECTED;
                bluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectionState = STATE_DISCONNECTED;
            }
            updateStatusIntent();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	Log.w(TAG,"Searching for send & receive characteristics");
            	
                List<BluetoothGattService> bgsList = bluetoothGatt.getServices();
                
                for(BluetoothGattService bgs : bgsList) {
                	Log.w(TAG,"Service UUID: "+bgs.getUuid().toString());
                    List<BluetoothGattCharacteristic> bgcList = bgs.getCharacteristics();
                    
                    for(BluetoothGattCharacteristic bgc : bgcList) {
                    	String uuid = bgc.getUuid().toString();
                    	Log.w(TAG,"Characteristic UUID: "+uuid);
                    	
                    	if(uuid.contains(RECEIVE_CHARACTERISTIC_UUID)) {
                    		Log.w(TAG,"RFduino receive characteristic found");
                    		receiveCharacteristic = bgc;
                    	} else if(uuid.contains(SEND_CHARACTERISTIC_UUID)) {
                    		Log.w(TAG,"RFduino send characteristic found");
                    		sendCharacteristic = bgc;
                    	}
                    }
                }
                
                if (receiveCharacteristic == null) {
                	Log.w(ERROR_TAG, "Receive characteristic not found");
                }
                if (sendCharacteristic == null) {
                	Log.w(ERROR_TAG, "Send characteristic not found");
                }
            } else {
                Log.w(ERROR_TAG, "onServicesDiscovered status " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
            	return;
            }
            
            Log.w(TAG,"Parsing read characteristic value");
        	try {
        		stringToDisplay = new String(characteristic.getValue(), "US-ASCII");
        	} catch (UnsupportedEncodingException e) {
				Log.w(ERROR_TAG, "Unsupported Encoding");
				stringToDisplay = "";
			}
        	updateResponseIntent();
        }
    };
    
    public void writeDataToCharacteristic(final BluetoothGattCharacteristic ch, final byte[] dataToWrite) {
        if (bluetoothAdapter == null || bluetoothGatt == null || ch == null) return;
        
        Log.w(TAG,"Writing");
        ch.setValue(dataToWrite);
        
        if(bluetoothGatt.writeCharacteristic(ch)) {
        	Log.w(TAG,"write success");
        } else {
        	Log.w(ERROR_TAG,"write failed");
        }
        
    }
    
    public void readDataFromCharacteristic(final BluetoothGattCharacteristic ch) {
    	if (bluetoothAdapter == null || bluetoothGatt == null || ch == null) return;
    	if(bluetoothGatt.readCharacteristic(ch)) {
        	Log.w(TAG,"read success");
        } else {
        	Log.w(ERROR_TAG,"read failed");
        }
    }
    
    public void updateStatusIntent() {
    	Log.w(TAG,"broadcasting status update intent");
    	Context context = getApplicationContext();
    	Intent i = new Intent();
        i.setAction(UPDATE_STATUS_INTENT);
        context.sendBroadcast(i);
    }
    
    public void updateResponseIntent() {
    	Log.w(TAG,"broadcasting response update intent");
    	Context context = getApplicationContext();
    	Intent i = new Intent();
        i.setAction(UPDATE_RESPONSE_INTENT);
        context.sendBroadcast(i);
    }
}
