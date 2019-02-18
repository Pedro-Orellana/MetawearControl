package com.example.metawearcontrol;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.builder.filter.ThresholdOutput;
import com.mbientlab.metawear.module.AccelerometerBosch;
import com.mbientlab.metawear.module.Switch;
import com.mbientlab.metawear.data.EulerAngles;
import com.mbientlab.metawear.module.SensorFusionBosch;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import bolts.Continuation;
import bolts.Task;
import static com.example.metawearcontrol.MainActivity.myBoard;


public class ConnectedActivity extends AppCompatActivity {
    private TextView connectedText;
    private AccelerometerBosch accBosch;
    private Switch pushButton;
    final static String ARDUINO_DEVICE = "arduino";
    private BluetoothLeScanner scanner;
    private BluetoothDevice btDevice;
    private BluetoothManager manager;
    private Button arduinoButton;
    private BluetoothGattService myGattService;
    private BluetoothGattCharacteristic TXCharacteristic;
    private String spokenText;
    private UUID TX_UUID;
    private UUID SERVICE_UUID;
    private Handler handler;
    private BluetoothGatt gatt;
    private TextView pressedButton;


    final SensorFusionBosch sensorFusion = myBoard.getModule(SensorFusionBosch.class);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected);
        connectedText = findViewById(R.id.connected_text);
        arduinoButton = findViewById(R.id.connected_button1);
        arduinoButton.setVisibility(View.GONE);

        accBosch = myBoard.getModule(AccelerometerBosch.class);
        pushButton = myBoard.getModule(Switch.class);
        pressedButton = findViewById(R.id.pressed_button);



        manager = (BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
        scanner = manager.getAdapter().getBluetoothLeScanner();


        TX_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
        SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");

        handler = new Handler();


        sensorFusion.configure()
                .mode(SensorFusionBosch.Mode.NDOF)
                .accRange(SensorFusionBosch.AccRange.AR_16G)
                .gyroRange(SensorFusionBosch.GyroRange.GR_2000DPS)
                .commit();



    }



    public void onClickTest(){
        TapTesting();

    }


    public void TapTesting (){

        accBosch.tap().configure()
                .enableDoubleTap()
                .threshold(2f)
                .shockTime(AccelerometerBosch.TapShockTime.TST_50_MS)
                .commit();
        accBosch.tap().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.stream(new Subscriber() {
                    @Override
                    public void apply(Data data, Object... env) {
                        AccelerometerBosch.Tap tap = data.value(AccelerometerBosch.Tap.class);
                        switch(tap.type) {
                            case SINGLE:
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        TXCharacteristic.setValue("A");
                                        gatt.writeCharacteristic(TXCharacteristic);

                                    }
                                });
                                break;
                            case DOUBLE:
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        connectedText.setText("A double tap has been detected");
                                        TXCharacteristic.setValue("B");
                                        gatt.writeCharacteristic(TXCharacteristic);

                                    }
                                });
                                break;
                        }
                    }
                });
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {
                accBosch.tap().start();
                accBosch.start();
                return null;
            }
        });
    }




    ScanCallback callback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            btDevice = result.getDevice();
            connectedText.setText("The Arduino board has been found!");
            arduinoButton.setVisibility(View.VISIBLE);



        }
    };




    public void onClickScan(View view) {
        List<ScanFilter> filters = new ArrayList<>();
        ScanFilter filter =  new ScanFilter.Builder().setDeviceName("Uno").build();
        filters.add(filter);

        ScanSettings settings = new ScanSettings.Builder().build();

        scanner.startScan(filters,settings,callback);

    }

    public void onClickConnectToArduino(View view){
        gatt = btDevice.connectGatt(this,true,gattCallback);
    }



    public void onDiscoverServices() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                gatt.discoverServices();
            }
        }, 600);
        //Snackbar.make(linearLayout, "Service discovered!", Snackbar.LENGTH_SHORT).show();

    }



    BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    onDiscoverServices();
                    //Snackbar.make(linearLayout, "Connected and ready to be used", Snackbar.LENGTH_SHORT);
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    //Snackbar.make(linearLayout, "The board is disconnected", Snackbar.LENGTH_SHORT);
                    break;


            }


        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if(status == BluetoothGatt.GATT_SUCCESS){
                myGattService = gatt.getService(SERVICE_UUID);
                TXCharacteristic = myGattService.getCharacteristic(TX_UUID);
                connectedText.setText("ready to go!");
                onClickTest();


            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            //messageTextView.setText("A message was sent!");

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
        }
    };

}
