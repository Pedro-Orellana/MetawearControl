package com.example.metawearcontrol2;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.mbientlab.metawear.MetaWearBoard;

public class MainActivity extends AppCompatActivity {

    private BluetoothManager manager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar_main_activity);
        setSupportActionBar(toolbar);
        manager = (BluetoothManager) this.getSystemService(BLUETOOTH_SERVICE);
    }

    public class ConnectToMetawearAsyncTask extends AsyncTask<BluetoothManager, Void, BluetoothDevice> {
        @Override
        protected BluetoothDevice doInBackground(BluetoothManager... bluetoothManagers) {


return null;
        }


    }
}
