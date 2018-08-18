package com.example.blebox;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.bluetooth.BluetoothDevice;
import android.app.Activity;

import com.example.blebox.Bluetooth.BluetoothChatService;
import com.example.blebox.Bluetooth.DeviceListActivity;
import com.example.blebox.utils.PreferenceUtil;
import com.tbruyelle.rxpermissions.RxPermissions;

import rx.functions.Action1;

import static com.example.blebox.Bluetooth.BluetoothChatService.DEVICE_NAME;
import static com.example.blebox.Bluetooth.BluetoothChatService.MESSAGE_DEVICE_NAME;
import static com.example.blebox.Bluetooth.BluetoothChatService.MESSAGE_READ;
import static com.example.blebox.Bluetooth.BluetoothChatService.MESSAGE_STATE_CHANGE;
import static com.example.blebox.Bluetooth.BluetoothChatService.MESSAGE_TOAST;
import static com.example.blebox.Bluetooth.BluetoothChatService.MESSAGE_WRITE;
import static com.example.blebox.Bluetooth.BluetoothChatService.TOAST;

public class MainActivity extends AppCompatActivity {

    private Button btnstart,btnstop,btnsettings;
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothChatService mChatService = null;
    private String mConnectedDeviceName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //control Button
        btnstart = (Button)findViewById(R.id.btn_start);
        btnstop = (Button)findViewById(R.id.btn_stop);
        btnsettings = (Button)findViewById(R.id.btn_settings);

        initListener();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "手机无蓝牙设备", Toast.LENGTH_SHORT).show();
        }else{
            if(!mBluetoothAdapter.isEnabled()){
                if (Build.VERSION.SDK_INT >= 23) {
                    RxPermissions.getInstance(MainActivity.this)
                            .request(Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION)
                            .subscribe(new Action1<Boolean>() {
                                @Override
                                public void call(Boolean aBoolean) {
                                    if (aBoolean) {
                                        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                        startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
                                    }
                                }
                            });
                } else {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
                }
            }else{
                // Initialize the BluetoothChatService to perform bluetooth connections
                mChatService = new BluetoothChatService(this, mHandler);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mChatService != null && mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                mChatService.start();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) mChatService.stop();
        if(mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()){
            mBluetoothAdapter.disable();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case REQUEST_CONNECT_DEVICE:
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    if(mChatService == null){
                        mChatService = new BluetoothChatService(this, mHandler);
                        mChatService.start();
                    }
                    mChatService.connect(device);
                }
                break;

            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, R.string.bt_enabled_success, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initListener(){

        btnstart.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        sendMessage(PreferenceUtil.getInstance().getUpCode());
                        break;

                    case MotionEvent.ACTION_UP:
                        sendMessage(PreferenceUtil.getInstance().getStopCode());
                        break;
                }
                return false;
            }
        });
        btnstop.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        sendMessage(PreferenceUtil.getInstance().getLeftCode());
                        break;
                    case MotionEvent.ACTION_UP:
                        sendMessage(PreferenceUtil.getInstance().getStopCode());
                        break;
                }
                return false;
            }
        });
        btnsettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent serverIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            }
        });


    @SuppressLint("HandlerLeak") final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            Toast.makeText(getApplicationContext(), "正在连接该蓝牙设备", Toast.LENGTH_SHORT).show();
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    String result = "";
                    if(writeMessage.equals(PreferenceUtil.getInstance().getStopCode())){
                        result = "停车";
                    }else if(writeMessage.equals(PreferenceUtil.getInstance().getLeftCode())){
                        result = "左转";
                    }else if(writeMessage.equals(PreferenceUtil.getInstance().getRightCode())){
                        result = "右转";
                    }else if(writeMessage.equals(PreferenceUtil.getInstance().getUpCode())){
                        result = "前进";
                    }else if(writeMessage.equals(PreferenceUtil.getInstance().getDownCode())){
                        result = "后退";
                    }
                    Log.d("蓝牙小车:",result);
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "连接上 "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

}

    private void sendMessage(String message) {
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "手机无蓝牙设备", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that we're actually connected before trying anything
        if (mChatService == null ||(mChatService.getState() != BluetoothChatService.STATE_CONNECTED)) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        // Check that there's actually something to send
        if (message.length() > 0) {
            byte[] send = message.getBytes();
            mChatService.write(send);
        }
    }
    }
