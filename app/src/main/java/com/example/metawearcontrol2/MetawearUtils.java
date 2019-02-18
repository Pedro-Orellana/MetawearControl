package com.example.metawearcontrol2;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;

import java.util.ArrayList;

import java.util.List;



public class MetawearUtils {

   private static final String METAWEAR_MAC_ADDRESS ="address";

    private static BluetoothDevice foundMetawearDevice;
    private static BluetoothLeScanner scanner;
    private static MetaWearScanCallback callback;


    static void startBluetoothScan(BluetoothManager manager){
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











    private static class MetaWearScanCallback extends ScanCallback{
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            foundMetawearDevice = result.getDevice();

            finishScan();



        }
    }

    private static void finishScan(){
        scanner.stopScan(callback);

    }


}
