package com.example.metawearcontrol2;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.data.EulerAngles;
import com.mbientlab.metawear.module.AccelerometerBosch;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.SensorFusionBosch;
import com.philips.lighting.hue.sdk.wrapper.HueLog;
import com.philips.lighting.hue.sdk.wrapper.Persistence;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnection;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnectionCallback;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnectionType;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeResponseCallback;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeStateUpdatedCallback;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeStateUpdatedEvent;
import com.philips.lighting.hue.sdk.wrapper.connection.ConnectionEvent;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscovery;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscoveryImpl;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscoveryResult;
import com.philips.lighting.hue.sdk.wrapper.domain.Bridge;
import com.philips.lighting.hue.sdk.wrapper.domain.BridgeBuilder;
import com.philips.lighting.hue.sdk.wrapper.domain.HueError;
import com.philips.lighting.hue.sdk.wrapper.domain.ReturnCode;
import com.philips.lighting.hue.sdk.wrapper.domain.clip.ClipResponse;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightPoint;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightState;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightStateImpl;
import com.philips.lighting.hue.sdk.wrapper.knownbridges.KnownBridge;
import com.philips.lighting.hue.sdk.wrapper.knownbridges.KnownBridges;
import com.philips.lighting.hue.sdk.wrapper.utilities.InitSdk;


import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import bolts.Continuation;
import bolts.Task;

public class ConnectedActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private MetaWearBoard myBoard;
    private TextView connectingTextView;
    private ProgressBar connectingProgressBar;
    private LinearLayout linearLayout;
    private TextView xTextView;
    private TextView yTextView;
    private TextView zTextView;

   // Creating private instances of sensors on board
    private AccelerometerBosch accBosch;
    private Led led;
    private SensorFusionBosch sensorFusion;


    private Handler handler;
    private Boolean alreadyWorked;
    private Boolean alreadyWorkedRoll;
    private int numberOfClicks;

    private Boolean firstStepDonePitch;
    private Boolean secondStepDonePitch;


//Set up objects required for lighting system integration
    private Bridge bridge;
    private BridgeDiscovery bridgeDiscovery;
    private List<BridgeDiscoveryResult> bridgeDiscoveryResults;
    private BridgeDiscoveryResult myBridge;
    private Toolbar toolbar;

    //implementation of liveData



    static {
        // Load the huesdk native library before calling any SDK method
        System.loadLibrary("huesdk");
    }

    // Life cycle methods

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected);
        //Getting references of the views
        connectingProgressBar = findViewById(R.id.connecting_progress_bar);
        connectingTextView = findViewById(R.id.connecting_text_view);
        linearLayout = findViewById(R.id.connected_linear_layout);
        xTextView = findViewById(R.id.orientation_x);
        yTextView = findViewById(R.id.orientation_y);
        zTextView = findViewById(R.id.orientation_z);
        toolbar = findViewById(R.id.connected_toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,drawerLayout,toolbar,R.string.open_drawer,R.string.close_drawer);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();



        //getting the BluetoothDevice passed on the intent that initiated this activity
        Intent intent = getIntent();
        BluetoothDevice device = (BluetoothDevice) intent.getExtras().get("BluetoothDevice");

        //Calling an AsyncTask to the process of connecting to the board
        ConnectingAsyncTask myTask = new ConnectingAsyncTask();
        myTask.execute(device);

        connectingProgressBar.setVisibility(View.VISIBLE);
        linearLayout.setVisibility(View.INVISIBLE);



        handler = new Handler();
        numberOfClicks = 0;
        secondStepDonePitch = false;

        //Setting up the lighting system
        InitSdk.setApplicationContext(getApplicationContext());

        // Configure the storage location and log level for the Hue SDK
        Persistence.setStorageLocation(getFilesDir().getAbsolutePath(), "MetawearControl");
        HueLog.setConsoleLogLevel(HueLog.LogLevel.INFO);

        String bridgeIp = getLastUsedBridgeIp();
        if (bridgeIp == null) {
            startBridgeConnection();
            yTextView.setText(bridgeIp);
        } else {
            MakeConnectionToBridge(bridgeIp);
        }



    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        myBoard.tearDown();
        myBoard.disconnectAsync();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()){
            case R.id.drawer_settings:
                Intent intent = new Intent(this,SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.drawer_about:
                //Empty body for now. Maybe add a fragment, activity or link to a web page in the future.

                break;

        }
        return false;
    }

    //AsyncTask that connects to the board.

    private class ConnectingAsyncTask extends AsyncTask<BluetoothDevice, Void, Task> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            connectingProgressBar.setVisibility(View.VISIBLE);
            linearLayout.setVisibility(View.INVISIBLE);
        }

        @Override
        protected Task doInBackground(BluetoothDevice... bluetoothDevices) {
            myBoard = MainActivity.myLocalBinder.getMetaWearBoard(bluetoothDevices[0]);
            return myBoard.connectAsync();
        }


        @Override
        protected void onPostExecute(Task task) {
            super.onPostExecute(task);
            Handler handler = new Handler();
            if (task.isFaulted()) {
                connectingTextView.setText("The app could not connect to the board :(");
                connectingProgressBar.setVisibility(View.INVISIBLE);
            }
            else {
                handler.postDelayed(()-> {
                    connectingTextView.setText("Board is connected and ready to go");
                   connectingProgressBar.setVisibility(View.INVISIBLE);
                  linearLayout.setVisibility(View.VISIBLE);
                    led = myBoard.getModule(Led.class);
                    sensorFusion = myBoard.getModule(SensorFusionBosch.class);
                    alreadyWorked = true;
                    setMySensors();
                    startBridgeConnection();
                },5000);

            }
        }

    }



    //New Code with implementation of liveData
    //Is it worth it though?

static class MetawearViewModel extends ViewModel{
        private MutableLiveData<BluetoothDevice> BoardLiveData;

        MutableLiveData<BluetoothDevice> getBoardLiveData(){
            if(BoardLiveData == null){
                BoardLiveData = new MutableLiveData<>();
            }
            return BoardLiveData;
    }
}





    //  Beginning of Hue light bulbs code section

    public void startBridgeConnection() {
        disconnectFromBridge();
        bridgeDiscovery = new BridgeDiscoveryImpl();
        bridgeDiscovery.search(BridgeDiscovery.Option.ALL, bridgeDiscoveryCallback);


    }

    private String getLastUsedBridgeIp() {
        List<KnownBridge> bridges = KnownBridges.getAll();


        if (bridges.isEmpty()) {
            return null;
        }

        return Collections.max(bridges, new Comparator<KnownBridge>() {
            @Override
            public int compare(KnownBridge a, KnownBridge b) {

                return a.getLastConnected().compareTo(b.getLastConnected());
            }
        }).getIpAddress();
    }

    private void disconnectFromBridge() {
        if (bridge != null) {
            bridge.disconnect();
            bridge = null;

        }
    }

    private BridgeDiscovery.Callback bridgeDiscoveryCallback = new BridgeDiscovery.Callback() {
        @Override
        public void onFinished(final List<BridgeDiscoveryResult> results, final BridgeDiscovery.ReturnCode returnCode) {
            // Set to null to prevent stopBridgeDiscovery from stopping it
            bridgeDiscovery = null;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (returnCode == BridgeDiscovery.ReturnCode.SUCCESS) {
                        //bridgeDiscoveryListView.setAdapter(new BridgeDiscoveryResultAdapter(getApplicationContext(), results));
                        bridgeDiscoveryResults = results;
                        myBridge = bridgeDiscoveryResults.get(0);
                        yTextView.setText(myBridge.getIp());


                        //updateUI(UIState.BridgeDiscoveryResults, "Found " + results.size() + " bridge(s) in the network.");
                    } else if (returnCode == BridgeDiscovery.ReturnCode.STOPPED) {
                        //Log.i(TAG, "Bridge discovery stopped.");
                        yTextView.setText("The discovery was stopped");

                    } else {
                        //updateUI(UIState.Idle, "Error doing bridge discovery: " + returnCode);
                        yTextView.setText("There was an error finding the bridge");
                    }
                }
            });
        }
    };

    public void makeConnectionToBridge(View view){
        stopBridgeDiscovery();
        disconnectFromBridge();
        bridge = new BridgeBuilder("app name", "device name")
                //.setIpAddress(myBridge.getIp())
                .setIpAddress(getLastUsedBridgeIp())
                .setConnectionType(BridgeConnectionType.LOCAL)

                // Working with some deprecated code. Maybe in need of finding newer methods.

                .setBridgeConnectionCallback(bridgeConnectionCallback)
                .addBridgeStateUpdatedCallback(bridgeStateUpdatedCallback)
                .build();

        bridge.connect();
    }

    private void stopBridgeDiscovery() {
        if (bridgeDiscovery != null) {
            bridgeDiscovery.stop();
            bridgeDiscovery = null;
        }
    }
    public void MakeConnectionToBridge(String BridgeIp){
        stopBridgeDiscovery();
        disconnectFromBridge();
        bridge = new BridgeBuilder("app name", "device name")
                .setIpAddress(BridgeIp)
                .setConnectionType(BridgeConnectionType.LOCAL)

                // Working with some deprecated code. Maybe in need of finding newer methods.

                .setBridgeConnectionCallback(bridgeConnectionCallback)
                .addBridgeStateUpdatedCallback(bridgeStateUpdatedCallback)
                .build();

        bridge.connect();
        if (bridge.isConnected()){
            xTextView.setText("The connection to the lights should be ready to go");
        }
    }

    private BridgeConnectionCallback bridgeConnectionCallback = new BridgeConnectionCallback() {
        @Override
        public void onConnectionEvent(BridgeConnection bridgeConnection, ConnectionEvent connectionEvent) {
            //Log.i(TAG, "Connection event: " + connectionEvent);

            switch (connectionEvent) {
                case LINK_BUTTON_NOT_PRESSED:
                    //updateUI(UIState.Pushlinking, "Press the link button to authenticate.");
                    break;

                case COULD_NOT_CONNECT:
                    runOnUiThread(()-> yTextView.setText("Could not connect"));

                    break;

                case CONNECTION_LOST:
                    runOnUiThread(()-> yTextView.setText("Connection lost. Attempting to reconnect"));

                    break;

                case CONNECTION_RESTORED:
                    runOnUiThread(()-> yTextView.setText("Connection restored."));

                    break;

                case DISCONNECTED:
                    // User-initiated disconnection.
                    break;

                default:
                    break;
            }
        }

        @Override
        public void onConnectionError(BridgeConnection bridgeConnection, List<HueError> list) {
            for (HueError error : list) {
                // Log.e(TAG, "Connection error: " + error.toString());
            }
        }
    };


    private BridgeStateUpdatedCallback bridgeStateUpdatedCallback = new BridgeStateUpdatedCallback() {
        @Override
        public void onBridgeStateUpdated(Bridge bridge, BridgeStateUpdatedEvent bridgeStateUpdatedEvent) {


            switch (bridgeStateUpdatedEvent) {
                case INITIALIZED:
                    // The bridge state was fully initialized for the first time.
                    // It is now safe to perform operations on the bridge state.
                    //updateUI(UIState.Connected, "Connected!");
                    //setupEntertainmentGroup();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            zTextView.setText("It is now safe to perform operations on the bridge state.");

                        }
                    });


                    break;

                case LIGHTS_AND_GROUPS:
                    break;

                default:
                    break;
            }
        }
    };



    public void onClickTurnOn(){
        List<LightPoint> lights = bridge.getBridgeState().getLights();
        for (final LightPoint light : lights) {
            final LightState lightState = new LightStateImpl();

            lightState.setOn(true);
            lightState.setBrightness(300);

            light.updateState(lightState, BridgeConnectionType.LOCAL, new BridgeResponseCallback() {
                @Override
                public void handleCallback(Bridge bridge, ReturnCode returnCode, List<ClipResponse> list, List<HueError> errorList) {
                    if (returnCode == ReturnCode.SUCCESS) {
                        runOnUiThread(()-> xTextView.setText("The lights should have turn on"));
                        // Log.i(TAG, "Changed hue of light " + light.getIdentifier() + " to " + lightState.getHue());
                    } else {
                        // Log.e(TAG, "Error changing hue of light " + light.getIdentifier());
                       runOnUiThread(()->{
                           xTextView.setText("There was an error :(");
                       });
                        for (HueError error : errorList) {
                            //Log.e(TAG, error.toString());
                        }
                    }
                }
            });
        }

    }

    public void onClickTurnOff(){
        List<LightPoint> lights = bridge.getBridgeState().getLights();
        for (final LightPoint light : lights) {
            final LightState lightState = new LightStateImpl();

            lightState.setOn(false);

            light.updateState(lightState, BridgeConnectionType.LOCAL, new BridgeResponseCallback() {
                @Override
                public void handleCallback(Bridge bridge, ReturnCode returnCode, List<ClipResponse> list, List<HueError> errorList) {
                    if (returnCode == ReturnCode.SUCCESS) {
                        runOnUiThread(()-> xTextView.setText("The lights should have turn off"));
                        // Log.i(TAG, "Changed hue of light " + light.getIdentifier() + " to " + lightState.getHue());
                    } else {
                        // Log.e(TAG, "Error changing hue of light " + light.getIdentifier());
                        xTextView.setText("There was an error :(");
                        for (HueError error : errorList) {
                            //Log.e(TAG, error.toString());
                        }
                    }
                }
            });
        }

    }

    // end of section of Hue light bulbs code









        public void onClickStop(View view) {
            accBosch = myBoard.getModule(AccelerometerBosch.class);
            if(accBosch != null){
                xTextView.setText("accBosh is set and ready to go");
                accBosch.tap().configure()
                        .enableDoubleTap()
                        .enableSingleTap()
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
                                        Log.i("MainActivity", "Single tap");
                                        led.stop(false);
                                        break;
                                    case DOUBLE:
                                        setMySensors2();
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
            }else{
                xTextView.setText("AccBosh not configured yet");
            }
        }


        public void setMySensors(){

            myBoard.tearDown();

            numberOfClicks = 0;
            alreadyWorked = true;
            alreadyWorkedRoll = true;

            accBosch = myBoard.getModule(AccelerometerBosch.class);
            led = myBoard.getModule(Led.class);

            if(accBosch != null){
                accBosch.tap().configure()
                        .enableDoubleTap()
                        .enableSingleTap()
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
                                        Log.i("MainActivity", "Single tap");
                                        led.stop(false);
                                        break;
                                    case DOUBLE:
                                        setMySensors2();
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
            }else{
                xTextView.setText("AccBosh not configured yet");
            }





//            accBosch.tap().configure()
//                    .enableDoubleTap()
//                    .enableSingleTap()
//                    .threshold(2f)
//                    .shockTime(AccelerometerBosch.TapShockTime.TST_50_MS)
//                    .commit();
//            accBosch.tap().addRouteAsync(new RouteBuilder() {
//                @Override
//                public void configure(RouteComponent source) {
//                    source.stream(new Subscriber() {
//                        @Override
//                        public void apply(Data data, Object... env) {
//                            AccelerometerBosch.Tap tap = data.value(AccelerometerBosch.Tap.class);
//                            switch(tap.type) {
//                                case SINGLE:
//                                    Log.i("MainActivity", "Single tap");
//                                    led.stop(false);
//                                    break;
//                                case DOUBLE:
//                                    setMySensors2();
//                                    break;
//                            }
//                        }
//                    });
//                }
//            }).continueWith(new Continuation<Route, Void>() {
//                @Override
//                public Void then(Task<Route> task) throws Exception {
//                    accBosch.tap().start();
//                    accBosch.start();
//                    return null;
//                }
//            });

        }



                public void setMySensors2(){
        //This is where most of the logic of the app is written.

                    myBoard.tearDown();

            firstStepDonePitch = false;
            secondStepDonePitch = false;

                    led = myBoard.getModule(Led.class);

            led.editPattern(Led.Color.GREEN, Led.PatternPreset.SOLID).commit();
            led.play();


            sensorFusion.configure()
                    .mode(SensorFusionBosch.Mode.NDOF)
                    .accRange(SensorFusionBosch.AccRange.AR_16G)
                    .gyroRange(SensorFusionBosch.GyroRange.GR_2000DPS)
                    .commit();



        sensorFusion.eulerAngles().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.stream(new Subscriber() {
                    @Override
                    public void apply(Data data, Object... env) {
                            doSingleClick(data);
                    }
                });
            }
        }).continueWith(new Continuation<Route, Void>() {
                        @Override
                        public Void then(Task<Route> task) throws Exception {
                            sensorFusion.eulerAngles().start();
                            sensorFusion.start();
                            return null;
                        }
                    });

        }






        private void readNumberOfClicks(){
        switch (numberOfClicks){
            case 1:
                numberOfClicks = 0;
                alreadyWorked = true;
                alreadyWorkedRoll = true;
                led.editPattern(Led.Color.GREEN, Led.PatternPreset.SOLID).commit();
                led.play();
                sensorFusion.start();

                break;
            case 2:
                numberOfClicks = 0;
                led.editPattern(Led.Color.BLUE, Led.PatternPreset.SOLID).commit();
                led.play();
                sensorFusion.start();
                 break;

        }



        }

        private void doSingleClick(Data data){
            Log.i("MainActivity", "heading = " + data.value(EulerAngles.class));
            if(data.value(EulerAngles.class).pitch() > 80.000){
                if(!alreadyWorked) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(!firstStepDonePitch) {
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        checkIfItMovedUp();
                                    }
                                }, 750);
                                led.stop(true);
                                led.editPattern(Led.Color.RED, Led.PatternPreset.SOLID).commit();
                                led.play();
                                firstStepDonePitch = true;
                            }

                        }
                    });


                }

            }else if(data.value(EulerAngles.class).pitch() < 80.000 && data.value(EulerAngles.class).pitch() > -65.000 ) {//there is probably nothing I can do here...
                if(alreadyWorked) {
                    alreadyWorked = false;
                }else if(firstStepDonePitch && !secondStepDonePitch) {
                    secondStepDonePitch = true;
                }
            } else if(data.value(EulerAngles.class).pitch() < -65.000) {
                if(!alreadyWorked) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onClickTurnOn();
                            xTextView.setText("Pitch up");
                            sensorFusion.stop();
                            led.stop(true);
                            alreadyWorked = true;
                            setMySensors();


                        }
                    });

                }
            }

            if(data.value(EulerAngles.class).roll() < -60.000 && !alreadyWorkedRoll){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        xTextView.setText("Rolled to the right");
                        sensorFusion.stop();
                        led.stop(true);
                        alreadyWorkedRoll = true;
                        setMySensors();
                    }
                });
            }else if (data.value(EulerAngles.class).roll() > 60.000 && !alreadyWorkedRoll){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        xTextView.setText("Rolled to the left");
                        sensorFusion.stop();
                        led.stop(true);
                        alreadyWorkedRoll = true;
                        setMySensors();
                    }
                });
            }else if (data.value(EulerAngles.class).roll()< 60.000 && data.value(EulerAngles.class).roll()> -60.000){
                if(alreadyWorkedRoll) {
                    alreadyWorkedRoll = false;
                }
            }

        }

        private void checkIfItMovedUp() {

            if (secondStepDonePitch) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        xTextView.setText("Pitch down then up");
                        led.stop(true);
                        sensorFusion.stop();
                        firstStepDonePitch = false;
                        secondStepDonePitch = false;
                        alreadyWorked = true;
                        setMySensors();

                    }
                });

            } else if(!secondStepDonePitch){

                runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onClickTurnOff();


                            sensorFusion.stop();
                            led.stop(true);
                            alreadyWorked = true;
                            firstStepDonePitch = false;
                            secondStepDonePitch = false;
                            setMySensors();


                        }

                    });
            }
        }







    }

