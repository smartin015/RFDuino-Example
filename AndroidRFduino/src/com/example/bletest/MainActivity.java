package com.example.bletest;

import java.util.List;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {
	 private final static String TAG = MainActivity.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private EditText editRed;
    private EditText editGreen;
    private EditText editBlue;
    private Button btnSend;
    private String MAC = "C6:DF:7D:96:83:28";
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		editRed = (EditText) findViewById(R.id.editRed);
		editGreen = (EditText) findViewById(R.id.editGreen);
	    editBlue = (EditText) findViewById(R.id.editBlue);
	    btnSend = (Button) findViewById(R.id.btnSend);
		
		mBluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
		
		//BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(MAC);
		//mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
		connect(MAC);
		btnSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	Log.w(TAG,"ALMOST THERE");
                List<BluetoothGattService> bgsList = mBluetoothGatt.getServices();//mBluetoothGatt.getServices();
                Log.w(TAG,"List size: "+ bgsList.size());
                for(int i = 0; i < bgsList.size();i++)
                {
                	Log.w(TAG,"Service UUID: "+bgsList.get(i).getUuid().toString());
                    List<BluetoothGattCharacteristic> bgcList = bgsList.get(i).getCharacteristics();
                    
                    for(int j = 0; j < bgcList.size();j++)
                    {
                    	Log.w(TAG,"Characteristic UUID: "+bgcList.get(j).getUuid().toString());
                    	if(bgcList.get(j).getUuid().toString().contains("2222"))
                    	{
                    		byte[] value = new byte[3];
                            value[0] = (byte) (Integer.parseInt(editRed.getText().toString()) & 0xFF);
                            value[1] = (byte) (Integer.parseInt(editGreen.getText().toString()) & 0xFF);
                            value[2] = (byte) (Integer.parseInt(editBlue.getText().toString()) & 0xFF);
                            
                        	Log.w(TAG,"Writing val to characteristic");
                        	writeDataToCharacteristic(bgcList.get(j), value);
                    	}
                    }
                	
                }
            }
        });
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectionState = STATE_CONNECTED;
                //Log.w(TAG,"");
                mBluetoothGatt.discoverServices();
                
                //gatt.getConnectedDevices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = STATE_DISCONNECTED;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	//txtStatus.setText("Connected");
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);

            	//txtStatus.setText("Disconnected");
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

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
        	Log.w(TAG,"+1");
        }
        else
        {
        	Log.w(TAG,"nope");
        }
        
    }
    
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }
}
