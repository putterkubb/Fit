package com.example.ptrawt.putter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class Main3Activity extends Activity implements Runnable
{
    protected static final String TAG = "TAG";
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    Button mScan;
    Button btnStart;
    TextView txtCount;
    int runState = 0; // 0=idle, 1=start, 2=running
    int zetaState = 0; // 0=0 degree, 1=90 degree
    float count = 0;
    BluetoothAdapter mBluetoothAdapter;
    private UUID applicationUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private ProgressDialog mBluetoothConnectProgressDialog;
    private BluetoothSocket mBluetoothSocket;
    BluetoothDevice mBluetoothDevice;

    int[] pStart = new int[4];
    public double Pmagnitude ; // for collect old magnitude
    public double zeta;
    private String leftString;
    @Override
    public void onCreate(Bundle mSavedInstanceState)
    {
        super.onCreate(mSavedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main3);
        mScan = (Button) findViewById(R.id.Scan);
        btnStart = (Button) findViewById(R.id.btnStart);
        txtCount = (TextView) findViewById(R.id.txtCount);
        mScan.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View mView)
            {
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (mBluetoothAdapter == null)
                {
                    Toast.makeText(Main3Activity.this, "Message1", Toast.LENGTH_LONG).show();
                }
                else
                {
                    if (!mBluetoothAdapter.isEnabled())
                    {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    }
                    else
                    {
                        ListPairedDevices();
                        Intent connectIntent = new Intent(Main3Activity.this, DeviceListActivity.class);
                        startActivityForResult(connectIntent, REQUEST_CONNECT_DEVICE);
                    }
                }
            }
        });
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runState = 1;
                zetaState = 0;
                btnStart.setEnabled(false);

            }
        });

        mScan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View mView) {
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (mBluetoothAdapter == null) {
                    Toast.makeText(Main3Activity.this, "Message2", Toast.LENGTH_LONG).show();
                } else {
                    if (!mBluetoothAdapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    } else {
                        ListPairedDevices();
                        Intent connectIntent = new Intent(Main3Activity.this, DeviceListActivity.class);
                        startActivityForResult(connectIntent, REQUEST_CONNECT_DEVICE);
                    }
                }
            }
        });
    }// onCreate

    public void onActivityResult(int mRequestCode, int mResultCode, Intent mDataIntent)
    {
        super.onActivityResult(mRequestCode, mResultCode, mDataIntent);

        switch (mRequestCode)
        {
            case REQUEST_CONNECT_DEVICE:
                if (mResultCode == Activity.RESULT_OK)
                {
                    Bundle mExtra = mDataIntent.getExtras();
                    String mDeviceAddress = mExtra.getString("DeviceAddress");
                    //log.v(TAG, "Coming incoming address " + mDeviceAddress);
                    mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
                    mBluetoothConnectProgressDialog = ProgressDialog.show(this, "Connecting...", mBluetoothDevice.getName() + " : " + mBluetoothDevice.getAddress(), true, false);
                    Thread mBlutoothConnectThread = new Thread(this);
                    mBlutoothConnectThread.start();
                    //pairToDevice(mBluetoothDevice); This method is replaced by progress dialog with thread
                }
                break;

            case REQUEST_ENABLE_BT:
                if (mResultCode == Activity.RESULT_OK)
                {
                    ListPairedDevices();
                    Intent connectIntent = new Intent(Main3Activity.this, DeviceListActivity.class);
                    startActivityForResult(connectIntent, REQUEST_CONNECT_DEVICE);
                }
                else
                {
                    Toast.makeText(Main3Activity.this, "Message", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    private void ListPairedDevices()
    {
        Set<BluetoothDevice> mPairedDevices = mBluetoothAdapter.getBondedDevices();
        if (mPairedDevices.size() > 0)
        {
            for (BluetoothDevice mDevice : mPairedDevices)
            {
                //log.v(TAG, "PairedDevices: " + mDevice.getName() + " " + mDevice.getAddress());
            }
        }
    }

    public void run()
    {
        try
        {
            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(applicationUUID);
            mBluetoothAdapter.cancelDiscovery();
            mBluetoothSocket.connect();

            /*InputStream inputStream = mBluetoothSocket.getInputStream();

            int byteCount = inputStream.available();
            if(byteCount > 0)
            {
                byte[] rawBytes = new byte[byteCount];
                inputStream.read(rawBytes);
                final String string=new String(rawBytes,"UTF-8");
                Log.e("tetet",string);
            }*/

            beginListenForData(mBluetoothSocket.getInputStream());
           /* ConnectedThread mConnectedThread = new ConnectedThread(mBluetoothSocket);
            mConnectedThread.start();*/

            mHandler.sendEmptyMessage(0);
        }
        catch (IOException eConnectException)
        {
            //log.d(TAG, "CouldNotConnectToSocket", eConnectException);
            closeSocket(mBluetoothSocket);
            return;
        }
    }

    void beginListenForData(final InputStream inputStream)
    {
        //final Handler handler = new Handler();
        //stopThread = false;
        byte[] buffer = new byte[1024];
        Thread thread  = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted()/* && !stopThread*/) {
                    try
                    {
                        int byteCount = inputStream.available();
                        if(byteCount > 0)
                        {
                            byte[] rawBytes = new byte[byteCount];
                            inputStream.read(rawBytes);
                            final String string=new String(rawBytes,"UTF-8");

                            String[] stringArray = string.split("_");
                            ArrayList<int[]> arrayListValues = new ArrayList<int[]>();
                            for(int i=0;i<stringArray.length;i++){
                                String inArray[]= stringArray[i].split(",");
                                int[] temp = new int[4];
                                if(inArray.length==4){
                                    try{
                                        for(int j=0;j<inArray.length;j++){
                                            temp[j] = Integer.parseInt(inArray[j]);
                                        }
                                        if (runState == 1) {
                                            runState = 2;
                                            for(int j=0;j<inArray.length;j++){
                                                pStart[j] = temp[j];
                                            }
                                        } else if (runState == 2) {
                                            arrayListValues.add(temp);
                                            double magnitude = Math.sqrt((Math.pow(temp[0], 2) + Math.pow(temp[1], 2) + Math.pow(temp[2], 2)));
                                            //zeta = Math.acos((temp[0]*pStart[0] + temp[1]*pStart[1] + temp[2]*pStart[2] )/( magnitude*Pmagnitude));
                                            zeta = Math.acos((temp[1]*pStart[1] )/( magnitude*Pmagnitude));
                                            // check move way change
                                            //Log.e("90", ""+zeta);
                                            if(/*zetaState == 0 && */zeta>=Math.PI/2.0){
                                                // move 90 degree
                                                Pmagnitude = magnitude;
                                                zetaState = 1;
                                                count += 0.5;
                                                for(int j=0;j<inArray.length;j++){
                                                    pStart[j] = temp[j];
                                                }
                                                Log.e("90", count+", "+zeta+", "+(Math.PI/2) + ", "+pStart[0] + ", "+pStart[1] + ", "+pStart[2]);
                                            }/* else if(zetaState == 1 && zeta<=0.0){
                                                Log.e("90", count+", "+zeta+", "+(Math.PI/2) + ", "+pStart[0] + ", "+pStart[1] + ", "+pStart[2]);
                                                zetaState = 0;
                                                count += 0.5;
                                                for(int j=0;j<inArray.length;j++){
                                                    pStart[j] = temp[j];
                                                }
                                            } */else {
                                                Pmagnitude = Pmagnitude + magnitude;
                                            }
                                            txtCount.setText(String.valueOf(10.0 - count));
                                            if (count >= 10.0) {
                                                count = 0;
                                                Log.e("90", "ssssssssssssssssssss");
                                                runState = 0;
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        txtCount.setText("10");
                                                        btnStart.setEnabled(true);
                                                    }
                                                });
                                            }
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    TextView Pmag = (TextView) findViewById(R.id.Pmag);
                                                    Pmag.setText(String.valueOf((int)zeta));
                                                }
                                            });
                                        }
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }


                                }
                                //Log.e("putter", arrayListValues.toString());
                            }


                            //Log.e("readdddddddd", string);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    TextView diplay = (TextView) findViewById(R.id.display);
                                    diplay.setText(string);
                                }
                            });

                            /*handler.post(new Runnable() {
                                public void run()
                                {
                                    //textView.append(string);
                                    Log.e("readdddddddd", string);
                                }
                            });*/

                        }
                    }
                    catch (IOException ex)
                    {
                        //stopThread = true;
                    }
                }
            }
        });

        thread.start();
    }

    private void closeSocket(BluetoothSocket nOpenSocket)
    {
        try
        {
            nOpenSocket.close();
            //log.d(TAG, "SocketClosed");
        }
        catch (IOException ex)
        {
            //log.d(TAG, "CouldNotCloseSocket");
        }
    }

    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            mBluetoothConnectProgressDialog.dismiss();
            Toast.makeText(Main3Activity.this, "DeviceConnected", Toast.LENGTH_SHORT).show();
        }
    };


    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            //log.e("connnnnnnnnnnnnnnnn", "connnnnnnnnnnnnnnnnnnnnnnnnnnnnnnn");
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
            byte[] buffer = new byte[1024];
            int begin = 0;
            int bytes = 0;
            while (true) {
                try {
                    bytes += mmInStream.read(buffer, bytes, buffer.length - bytes);
                    for(int i = begin; i < bytes; i++) {
                        if(buffer[i] == "#".getBytes()[0]) {
                            mHandler.obtainMessage(1, begin, i, buffer).sendToTarget();
                            begin = i + 1;
                            if(i == bytes - 1) {
                                bytes = 0;
                                begin = 0;
                            }
                        }
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }

        Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                byte[] writeBuf = (byte[]) msg.obj;
                //log.e("hannnnd", new String(writeBuf));
                int begin = (int)msg.arg1;
                int end = (int)msg.arg2;

                switch(msg.what) {
                    case 1:
                        String writeMessage = new String(writeBuf);
                        writeMessage = writeMessage.substring(begin, end);
                        break;
                }
            }
        };
    }



}