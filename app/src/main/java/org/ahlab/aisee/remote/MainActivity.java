package org.ahlab.aisee.remote;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.ahlab.aisee.shared.models.BTMessageBody;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_ENABLE_BT=1;
    BluetoothAdapter bluetoothAdapter;
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final int MESSAGE_READ=0;
    public static final int MESSAGE_WRITE=1;
    public static final int CONNECTING=2;
    public static final int CONNECTED=3;
    public static final int DISCONNECTING=4;
    public static final int DISCONNECTED=5;
    public static final String deviceName = "QCOM-BTD";
    private boolean isConnected = false;
    public static final String TAG = MainActivity.class.getSimpleName();

    private Switch remoteSwitch;
    private Button sendButton;
    private Button wifiButton;
    private Button clickButton;
    private EditText ssid;
    private EditText password;

    ConnectThread connectThread;
    ConnectedThread connectedThread;

    private BluetoothDevice device;
    private BluetoothSocket mmSocket;


    String bluetooth_message="AISEEEEEEEEEE";


    @SuppressLint("HandlerLeak")
    Handler mHandler=new Handler()
    {
        @Override
        public void handleMessage(Message msg_type) {
            super.handleMessage(msg_type);

            switch (msg_type.what){
                case MESSAGE_READ:

                    byte[] readbuf=(byte[])msg_type.obj;

                    try {
                        BTMessageBody msg = BTMessageBody.deserialize(readbuf);
                        handleBTMessage(msg);
                    }
                    catch (Exception e){Log.e(TAG, "Could't read incoming message");}
                    //String string_recieved=new String(readbuf);
                    break;

                case MESSAGE_WRITE:

                    if(msg_type.obj!=null){
                        ConnectedThread connectedThread=new ConnectedThread((BluetoothSocket)msg_type.obj);
                        //connectedThread.write(bluetooth_message.getBytes());
                    }
                    break;

                case CONNECTED:
                    Toast.makeText(getApplicationContext(),"Connected",Toast.LENGTH_SHORT).show();
                    break;

                case CONNECTING:
                    Toast.makeText(getApplicationContext(),"Connecting...",Toast.LENGTH_SHORT).show();
                    break;

                case DISCONNECTING:
                    Toast.makeText(getApplicationContext(),"Disconnecting...",Toast.LENGTH_SHORT).show();
                    break;

                case DISCONNECTED:
                    Toast.makeText(getApplicationContext(),"Disconnected",Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerReceiver(btConnectionReceiver, getIntentFilters());

        remoteSwitch = (Switch) findViewById(R.id.switch1);
        sendButton = (Button) findViewById(R.id.button);
        wifiButton = (Button) findViewById(R.id.wifiButton);
        clickButton = (Button) findViewById(R.id.clickButton);
        ssid = (EditText) findViewById(R.id.ssid);
        password = (EditText) findViewById(R.id.password);
        initialize_bluetooth();

        remoteSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    initialize_connection();
                }
                else {
                    mHandler.obtainMessage(DISCONNECTING).sendToTarget();
                    closeSockets();
                }
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isConnected){
                    //connectedThread.write(bluetooth_message.getBytes());
                    BTMessageBody message = new BTMessageBody(Constants.CTRL_DEBUG);
                    message.setData(bluetooth_message);
                    connectedThread.write(message);
                }
            }
        });

        wifiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!"".equals(ssid.getText().toString().trim()) && isConnected){
                    BTMessageBody setupMessage = new BTMessageBody(Constants.PROG_WIFI);

                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put(Constants.KEY_TEXT, bluetooth_message);
                        jsonObject.put(Constants.KEY_SSID, ssid.getText().toString());
                        jsonObject.put(Constants.KEY_PWD, password.getText().toString());
                        setupMessage.setData(jsonObject.toString());
                    }
                    catch (Exception e){Log.e(TAG, "Failed to send data");}

                    connectedThread.write(setupMessage);
                }
            }
        });

        clickButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isConnected){
                    BTMessageBody message = new BTMessageBody(Constants.CTRL_CLICK);
                    connectedThread.write(message);
                }
            }
        });
    }

    private final BroadcastReceiver btConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)){
                isConnected = true;
                connectedThread = new ConnectedThread(connectThread.mmSocket);
                mHandler.obtainMessage(CONNECTED).sendToTarget();
            }
            else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)){
                isConnected = false;
                remoteSwitch.setChecked(false);
                mHandler.obtainMessage(DISCONNECTED).sendToTarget();
            }
            else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON){
                        setBluetoothDevice(bluetoothAdapter);
                }
            }
        }
    };

    public void initialize_connection()
    {
                connectThread = new ConnectThread(device);
                connectThread.start();
                Log.i("Connection Status", "******connected");
    }


    public void initialize_bluetooth()
    {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Toast.makeText(getApplicationContext(),"Your Device doesn't support bluetooth.", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else {
            setBluetoothDevice(bluetoothAdapter);
        }
    }


    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // MY_UUID is the app's UUID string, also used by the headset app code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket.
                mHandler.obtainMessage(CONNECTING).sendToTarget();

                mmSocket.connect();
            } catch (IOException connectException) {
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
    private class ConnectedThread extends Thread {

        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();

                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write (BTMessageBody buffer){
            try {
                //mmOutStream.write(bytes);
                mmOutStream.write(buffer.serialize());
            } catch (IOException e) { }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private void setBluetoothDevice(BluetoothAdapter bluetoothAdapter){
        for (BluetoothDevice bluetoothDevice : bluetoothAdapter.getBondedDevices()){
            if (bluetoothDevice.getName().equals(deviceName)){
                device = bluetoothDevice;
                break;
            }
        }
    }

    private void closeSockets(){
        if (mmSocket != null) {
            try {
                mmSocket.close();
                connectThread.mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "*************socket close failed");
            }
        }
    }

    private void handleBTMessage (BTMessageBody btMessageBody) throws Exception {
        switch (btMessageBody.getMessageID()){

        }
    }

    private IntentFilter getIntentFilters()
    {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);


        return intentFilter;
    }

    @Override
    protected void onStop() {
        //unregisterReceiver(btConnectionReceiver);
        closeSockets();
        remoteSwitch.setChecked(false);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(btConnectionReceiver);
        super.onDestroy();
    }
}