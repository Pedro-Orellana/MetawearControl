package com.example.metawearcontrol2;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;

import com.mbientlab.metawear.MetaWearBoard;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private BluetoothManager manager;
    private BluetoothLeScanner scanner;
    private final String METAWEAR_MAC_ADDRESS = "CB:42:F6:6E:2C:7A";
    private BluetoothDevice foundMetawearDevice;
    private MetaWearScanCallback callback;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar_main_activity);
        setSupportActionBar(toolbar);
        manager = (BluetoothManager) this.getSystemService(BLUETOOTH_SERVICE);
        progressBar = findViewById(R.id.scanning_progress_bar);
        recyclerView = findViewById(R.id.recycler_scan_results);
    }

    public class ConnectToMetawearAsyncTask extends AsyncTask<BluetoothManager, Void, Void> {
        @Override
        protected Void doInBackground(BluetoothManager... bluetoothManagers) {
            startBluetoothScan(bluetoothManagers[0]);
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            recyclerView.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);


        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if(foundMetawearDevice != null){


            }

        }
    }






    private void startBluetoothScan(BluetoothManager manager){
        scanner = manager.getAdapter().getBluetoothLeScanner();
        List<ScanFilter> filters = new ArrayList<>() ;
        ScanFilter filter =  new ScanFilter.Builder()
                .setDeviceAddress(METAWEAR_MAC_ADDRESS)
                .build();
        filters.add(filter);

        ScanSettings settings = new ScanSettings.Builder().build();

        callback = new MetaWearScanCallback();

        scanner.startScan(filters,settings,callback);




    }


    private class MetaWearScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            foundMetawearDevice = result.getDevice();
            scanner.stopScan(callback);




        }
    }

}
