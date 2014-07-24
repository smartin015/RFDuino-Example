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

    private BluetoothAdapter mBluetoothAdapter; 
    private BluetoothManager mBluetoothManager;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic sendCharacteristic = null;
    private BluetoothGattCharacteristic receiveCharacteristic = null;
    
    
    private String mBluetoothDeviceAddress;
    
    private String mConnectionState = STATE_DISCONNECTED;
    private static final String STATE_DISCONNECTED = "Disconnected";
    private static final String STATE_CONNECTING = "Connecting...";
    private static final String STATE_CONNECTED = "Connected";

    private static final String UPDATE_STATUS_INTENT = "ble.echo.update.STATUS";
    private static final String UPDATE_RESPONSE_INTENT = "ble.echo.update.RESPONSE";
    
    private static final String RECEIVE_UUID = "2222";
    private static final String SEND_UUID = "2221";
    
    private boolean mScanning;
    private Handler mHandler;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private String stringToDisplay = "Response";
    
    Button btnRefreshConnection;
    Button btnSend;
    EditText editToSend;
    TextView textResponse;
    TextView textConnectionStatus;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        IntentFilter filter = new IntentFilter();
        filter.addAction(UPDATE_STATUS_INTENT);
        registerReceiver(statusReceiver, filter);
        

        IntentFilter filter2 = new IntentFilter();
        filter2.addAction(UPDATE_RESPONSE_INTENT);
        registerReceiver(responseReceiver, filter2);
        
		setContentView(R.layout.activity_main);
		btnRefreshConnection = (Button) findViewById(R.id.btnRefreshConnection);
	    btnSend = (Button) findViewById(R.id.btnSend);
	    editToSend = (EditText) findViewById(R.id.editToSend);
	    textResponse = (TextView) findViewById(R.id.textResponse);
	    textConnectionStatus = (TextView) findViewById(R.id.textConnectionStatus);
	    
		Log.w(TAG,"Good start");
		// Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
    		Log.w(ERROR_TAG,"BLE not supported");
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.

		Log.w(TAG,"Making bluetoothmanager");
        mBluetoothManager =
            (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		Log.w(TAG,"making bluetoothadapter");
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
    		Log.w(ERROR_TAG,"Bluetooth not supported");
            finish();
        }
        Log.w(TAG, "Instantiating handler");
        mHandler = new Handler();
        
		
        //scanLeDevice(true);
        
        btnRefreshConnection.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	if(mScanning) {
            		Log.w(TAG,"Already Scannign, dumbass. And you misspelled scanning.");
            	}
            	else if(mConnectionState.equals(STATE_CONNECTED)) {
            		
            	}
            	else {
            		//Log.w(TAG,mBluetoothGatt.getConnectedDevices() .getDevice().getName());
                	//mBluetoothGatt.disconnect();
            		Log.w(TAG,"Starting Scan");
            		scanLeDevice(true);
            	}
            }
        });
        
        btnSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	if(sendCharacteristic == null)
            	{
            		
            		
            	}
            	else if(receiveCharacteristic == null)
            	{
            		
            	}
            	else if(mConnectionState.equals(STATE_DISCONNECTED))
            	{
            		Log.w(ERROR_TAG,"You're not connected!");
            	}
            	else
            	{
            		writeDataToCharacteristic(receiveCharacteristic,stringToBytesASCII(editToSend.getText().toString()));
            	}
            	readDataFromCharacteristic(sendCharacteristic);
            }
        });
	}
	
	 private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }
	
	 // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                	Log.w(TAG, "Found device: " + device.getName());
                	if(device.getName().equals("RFduino"))
                	{
	                	Log.w(TAG,device.getAddress());
	                	connect(device.getAddress());
                	}
                	//new device! check it
                    //mLeDeviceListAdapter.addDevice(device);
                    //mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(ERROR_TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                updateStatusIntent();
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(ERROR_TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        updateStatusIntent();
        return true;
    }
    
    private BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	if (intent.getAction().equals(UPDATE_STATUS_INTENT)) {
                Log.w(TAG,"GOT THE STATUS INTENT");
            	textConnectionStatus.setText(mConnectionState);
            }
        }
    };
    
    private BroadcastReceiver responseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	if (intent.getAction().equals(UPDATE_RESPONSE_INTENT)) {
                Log.w(TAG,"GOT THE RESPONSE INTENT");
            	textResponse.setText(stringToDisplay);
            }
        }
    };
    
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        	Context context = getApplicationContext();
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectionState = STATE_CONNECTED;
                //Log.w(TAG,"");
                updateStatusIntent();
                //gatt.getConnectedDevices();

                mBluetoothGatt.discoverServices();

                
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = STATE_DISCONNECTED;
                updateStatusIntent();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	//txtStatus.setText("Connected");
            	Log.w(TAG,"ALMOST THERE");
                List<BluetoothGattService> bgsList = mBluetoothGatt.getServices();//mBluetoothGatt.getServices();
                Log.w(TAG,"List size: "+ bgsList.size());
                for(int i = 0; i < bgsList.size();i++) {
                	Log.w(TAG,"Service UUID: "+bgsList.get(i).getUuid().toString());
                    List<BluetoothGattCharacteristic> bgcList = bgsList.get(i).getCharacteristics();
                    for(int j = 0; j < bgcList.size();j++) {
                    	Log.w(TAG,"Characteristic UUID: "+bgcList.get(j).getUuid().toString());
                    	if(bgcList.get(j).getUuid().toString().contains(RECEIVE_UUID)) {
                    		Log.w(TAG,"RFduino receive characteristic found");
                    		receiveCharacteristic = bgcList.get(j);
                    	} else if(bgcList.get(j).getUuid().toString().contains(SEND_UUID)) {
                    		Log.w(TAG,"RFduino send characteristic found");
                    		sendCharacteristic = bgcList.get(j);
                    	}
                    }
                }
            } else {
                Log.w(ERROR_TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	stringToDisplay = "";
            	String tempChar = "a";
            	Log.w(TAG,"read some shit");
            	//characteristic.getValue()
            	byte[] tempByteArr = characteristic.getValue();
            	for(int i = 0; i<tempByteArr.length;i++)
            	{
                	try {
                		tempChar = new String(new byte[]{ tempByteArr[i] }, "US-ASCII");
						Log.w(TAG,"Char read: " + tempChar);
						stringToDisplay += tempChar;
					} catch (UnsupportedEncodingException e) {
						Log.w(ERROR_TAG, "Problem translating byte to string");
						// TODO Auto-generated catch block 
						e.printStackTrace();
					}
            	}
            	updateResponseIntent();
            } 
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {

        }
    };
    
    public void writeDataToCharacteristic(final BluetoothGattCharacteristic ch, final byte[] dataToWrite) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null || ch == null) return;
        Log.w(TAG,"Writing!");
        ch.setValue(dataToWrite);
        
        if(mBluetoothGatt.writeCharacteristic(ch))
        {
        	Log.w(TAG,"write successful");
        }
        else
        {
        	Log.w(ERROR_TAG,"write NOT successful");
        }
        
    }
    
    public void readDataFromCharacteristic(final BluetoothGattCharacteristic ch) {
    	if (mBluetoothAdapter == null || mBluetoothGatt == null || ch == null) return;
    	if(mBluetoothGatt.readCharacteristic(ch))
        {
        	Log.w(TAG,"read successful");
        }
        else
        {
        	Log.w(ERROR_TAG,"read NOT successful");
        }
    }
    
    public void updateStatusIntent()
    {
    	Log.w(TAG,"broadcasting status update intent");
    	Context context = getApplicationContext();

        context = getApplicationContext();
    	Intent i = new Intent();
        i.setAction(UPDATE_STATUS_INTENT);
        context.sendBroadcast(i);
    }
    
    public void updateResponseIntent()
    {
    	Log.w(TAG,"broadcasting response update intent");
    	Context context = getApplicationContext();

        context = getApplicationContext();
    	Intent i = new Intent();
        i.setAction(UPDATE_RESPONSE_INTENT);
        context.sendBroadcast(i);
    }
    
    
    
    public static byte[] stringToBytesASCII(String str) {
    	 char[] buffer = str.toCharArray();
    	 byte[] b = new byte[buffer.length];
    	 for (int i = 0; i < b.length; i++) {
    	  b[i] = (byte) buffer[i];
    	 }
    	 return b;
    	}
}
