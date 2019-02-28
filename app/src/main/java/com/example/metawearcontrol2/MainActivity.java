package com.example.metawearcontrol2;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.design.button.MaterialButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.android.BtleService;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ServiceConnection {

    static final String METAWEAR_MAC_ADDRESS = "CB:42:F6:6E:2C:7A";
    private BluetoothDevice foundMetawearDevice;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    static BtleService.LocalBinder myLocalBinder;
    private TextView scanningTextView;


//This has the onClick for the recyclerView.
    interface onClickImplementation{
        void myOnClick();

    }

    private onClickImplementation MainActivityClickImplementation = new onClickImplementation() {
        @Override
        public void myOnClick() {
            Intent intent = new Intent(MainActivity.this,ConnectedActivity.class);
            intent.putExtra("BluetoothDevice",foundMetawearDevice);
            startActivity(intent);


        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

       //Finding the toolbar and showing it on screen
        Toolbar toolbar = findViewById(R.id.toolbar_main_activity);
        setSupportActionBar(toolbar);

        //getting the Bluetooth manager to do the scan
        BluetoothManager bluetoothManager = (BluetoothManager) this.getSystemService(BLUETOOTH_SERVICE);

       //getting a reference of the views
        progressBar = findViewById(R.id.scanning_progress_bar);
        progressBar.setVisibility(View.VISIBLE);
        recyclerView = findViewById(R.id.recycler_scan_results);
        scanningTextView = findViewById(R.id.scanning_text_view);

        //getting the service and binding it to the activity
        bindService(new Intent(this, BtleService.class),this, Context.BIND_AUTO_CREATE);

       //start scanning for the ring
        if(bluetoothManager != null) {
            startBluetoothScan(bluetoothManager);

        }
    }


    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        myLocalBinder = (BtleService.LocalBinder) service;

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }





    private void startBluetoothScan(BluetoothManager manager){
        BluetoothLeScanner scanner = manager.getAdapter().getBluetoothLeScanner();
        if(scanner == null){
            scanningTextView.setText("the scanner is null");
        }else {
            List<ScanFilter> filters = new ArrayList<>();
            ScanFilter filter = new ScanFilter.Builder()
                    .setDeviceAddress(METAWEAR_MAC_ADDRESS)
                    .build();
            filters.add(filter);

            ScanSettings settings = new ScanSettings.Builder().build();

           MetaWearScanCallback callback = new MetaWearScanCallback();

            scanner.startScan(filters, settings, callback);


        }

    }


    private class MetaWearScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            foundMetawearDevice = result.getDevice();
            if(foundMetawearDevice != null) {
                String deviceName = foundMetawearDevice.getName();
                String deviceAdress = foundMetawearDevice.getAddress();
                int deviceType = foundMetawearDevice.getType();

                RecyclerViewAdapter recyclerViewAdapter = new RecyclerViewAdapter(deviceName, deviceAdress, deviceType, MainActivityClickImplementation);
                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.this);
                recyclerView.setLayoutManager(linearLayoutManager);
                recyclerView.setAdapter(recyclerViewAdapter);
                progressBar.setVisibility(View.INVISIBLE);
                recyclerView.setVisibility(View.VISIBLE);
            }









            }
    }

}
