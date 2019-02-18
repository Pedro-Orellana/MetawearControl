package com.example.metawearcontrol;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.TextView;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.android.BtleService;


import bolts.Continuation;
import bolts.Task;

public class MainActivity extends Activity implements ServiceConnection {

    static MetaWearBoard myBoard;
    public BtleService.LocalBinder myBinder;
    private final String MAC_ADDRESS = "CB:42:F6:6E:2C:7A";
    public final String METAWEAR_BOARD = "metawear board";
    private TextView errorText;






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        errorText = findViewById(R.id.error_text);



        bindService(new Intent(this, BtleService.class),
                this, Context.BIND_AUTO_CREATE);


    }

    public void retrieveBoard() {
        final BluetoothManager btManager=
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothDevice remoteDevice=
                btManager.getAdapter().getRemoteDevice(MAC_ADDRESS);

        myBoard= myBinder.getMetaWearBoard(remoteDevice);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getApplicationContext().unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        myBinder = (BtleService.LocalBinder) binder;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
    }




    public void connectMethod(){
        myBoard.connectAsync().continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(Task<Void> task) throws Exception {
                if (task.isFaulted()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            errorText.setText("The board could not connect to the phone");
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(MainActivity.this,ConnectedActivity.class);
                            startActivity(intent);

                        }
                    });
                }
                return null;
            }
        });
    }


    public void onClickConnect(View view){
        retrieveBoard();
        connectMethod();
    }







}


