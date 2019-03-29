package com.example.metawearcontrol2;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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
import com.mbientlab.metawear.module.Switch;
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
import com.philips.lighting.hue.sdk.wrapper.utilities.InitSdk;


import java.util.List;

import bolts.Continuation;
import bolts.Task;

public class ConnectedActivity extends AppCompatActivity {
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
    private Switch pushButton;


    private String buttonPressed;
    private Handler handler;
    private Boolean alreadyWorked;
    private Boolean alreadyWorkedRoll;
    private Boolean testingRunning;
    private Boolean isSingleClickActivated;
    private Boolean isDoubleClickActivated;
    private int numberOfClicks;

    private Boolean firstStepDonePitch;
    private Boolean secondStepDonePitch;


//Set up objects required for lighting system integration
    private Bridge bridge;
    private BridgeDiscovery bridgeDiscovery;
    private List<BridgeDiscoveryResult> bridgeDiscoveryResults;
    private BridgeDiscoveryResult myBridge;


    static {
        // Load the huesdk native library before calling any SDK method
        System.loadLibrary("huesdk");
    }

    // Life cycle methods

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected);
        connectingProgressBar = findViewById(R.id.connecting_progress_bar);
        connectingTextView = findViewById(R.id.connecting_text_view);
        linearLayout = findViewById(R.id.connected_linear_layout);
        xTextView = findViewById(R.id.orientation_x);
        yTextView = findViewById(R.id.orientation_y);
        zTextView = findViewById(R.id.orientation_z);

        Intent intent = getIntent();
        BluetoothDevice device = (BluetoothDevice) intent.getExtras().get("BluetoothDevice");
        ConnectingAsyncTask myTask = new ConnectingAsyncTask();
        myTask.execute(device);
        handler = new Handler();
        isSingleClickActivated = false;
        isDoubleClickActivated = false;
        numberOfClicks = 0;
        testingRunning = false;
        secondStepDonePitch = false;

        //Setting up the lighting system
        InitSdk.setApplicationContext(getApplicationContext());

        // Configure the storage location and log level for the Hue SDK
        Persistence.setStorageLocation(getFilesDir().getAbsolutePath(), "MetawearControl");
        HueLog.setConsoleLogLevel(HueLog.LogLevel.INFO);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myBoard.tearDown();
        myBoard.disconnectAsync();
    }

    //  Beginning of Hue light bulbs code section

    public void startBridgeConnection() {
        disconnectFromBridge();
        bridgeDiscovery = new BridgeDiscoveryImpl();
        bridgeDiscovery.search(BridgeDiscovery.Option.ALL, bridgeDiscoveryCallback);


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
        bridge = new BridgeBuilder("app name", "device name")
                .setIpAddress(myBridge.getIp())
                .setConnectionType(BridgeConnectionType.LOCAL)

                // Working with some deprecated code. Maybe in need of finding newer methods.

                .setBridgeConnectionCallback(bridgeConnectionCallback)
                .addBridgeStateUpdatedCallback(bridgeStateUpdatedCallback)
                .build();

        bridge.connect();
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
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            yTextView.setText("Could not connect");
                        }
                    });

                    // updateUI(UIState.Connecting, "Could not connect.");
                    break;

                case CONNECTION_LOST:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            yTextView.setText("Connection lost. Attempting to reconnect");
                        }
                    });
                    //updateUI(UIState.Connecting, "Connection lost. Attempting to reconnect.");
                    break;

                case CONNECTION_RESTORED:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            yTextView.setText("Connection restored.");
                        }
                    });
                    //updateUI(UIState.Connected, "Connection restored.");
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
            //Log.i(TAG, "Bridge state updated event: " + bridgeStateUpdatedEvent);

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
                    // At least one light was updated.
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
            lightState.setBrightness(100);

            light.updateState(lightState, BridgeConnectionType.LOCAL, new BridgeResponseCallback() {
                @Override
                public void handleCallback(Bridge bridge, ReturnCode returnCode, List<ClipResponse> list, List<HueError> errorList) {
                    if (returnCode == ReturnCode.SUCCESS) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                xTextView.setText("The lights should have turn off");
                            }
                        });
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

    public void onClickTurnOff(){
        List<LightPoint> lights = bridge.getBridgeState().getLights();
        for (final LightPoint light : lights) {
            final LightState lightState = new LightStateImpl();

            lightState.setOn(false);

            light.updateState(lightState, BridgeConnectionType.LOCAL, new BridgeResponseCallback() {
                @Override
                public void handleCallback(Bridge bridge, ReturnCode returnCode, List<ClipResponse> list, List<HueError> errorList) {
                    if (returnCode == ReturnCode.SUCCESS) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                xTextView.setText("The lights should have turn off");
                            }
                        });
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
            } else {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        connectingTextView.setText("Board is connected and ready to go");
                        connectingProgressBar.setVisibility(View.INVISIBLE);
                        linearLayout.setVisibility(View.VISIBLE);
                        led = myBoard.getModule(Led.class);
                        sensorFusion = myBoard.getModule(SensorFusionBosch.class);
                        pushButton = myBoard.getModule(Switch.class);
                        alreadyWorked = true;
                         setMySensors();
                        startBridgeConnection();
                    }
                },5000);

            }
        }

    }



        public void onClickStop(View view) {
        led.stop(true);
        }


        public void setMySensors(){

            myBoard.tearDown();

            numberOfClicks = 0;
            isSingleClickActivated = true;
            isDoubleClickActivated = false;
            testingRunning = false;
            alreadyWorked = true;
            alreadyWorkedRoll = true;

            accBosch = myBoard.getModule(AccelerometerBosch.class);
            led = myBoard.getModule(Led.class);





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

        }



                public void setMySensors2(){
        //This is where most of the logic of the app is written.

                    myBoard.tearDown();

            firstStepDonePitch = false;
            secondStepDonePitch = false;

                    led = myBoard.getModule(Led.class);

            led.editPattern(Led.Color.GREEN, Led.PatternPreset.SOLID).commit();
            led.play();
            //sensorFusion.eulerAngles().start();


            // push button sensor, will come back to it later

//            pushButton.state().addRouteAsync(new RouteBuilder() {
//                @Override
//                public void configure(RouteComponent source) {
//                    source.stream(new Subscriber() {
//                        @Override public void apply(Data data, Object... env) {
//                            Log.i("MainActivity",data.types().toString());
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    buttonPressed = data.toString().substring(47,48);
//                                    if(buttonPressed.equals("1")){
//                                        if(!testingRunning) {
//                                            testingRunning = true;
//                                            handler.postDelayed(new Runnable() {
//                                                @Override
//                                                public void run() {
//                                                    readNumberOfClicks();
//                                                }
//                                            },300);
//
//                                        }
//                                        numberOfClicks = numberOfClicks +1;
//                                    }
//
//                                }
//                            });
//
//                        }
//                    });
//                }
//            });

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
                        //if(isSingleClickActivated){
                            doSingleClick(data);
                       // } else if (isDoubleClickActivated){
                        //}

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
                isSingleClickActivated = true;
                isDoubleClickActivated = false;
                testingRunning = false;
                alreadyWorked = true;
                alreadyWorkedRoll = true;
                led.editPattern(Led.Color.GREEN, Led.PatternPreset.SOLID).commit();
                led.play();
                sensorFusion.start();

                break;
            case 2:
                numberOfClicks = 0;
                testingRunning = false;
                isDoubleClickActivated = true;
                isSingleClickActivated = false;
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
                            isSingleClickActivated = false;
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
                        isSingleClickActivated = false;
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
                        isSingleClickActivated = false;
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
                        isSingleClickActivated = false;
                        alreadyWorked = true;
                        setMySensors();

                    }
                });

            } else if(!secondStepDonePitch){

                runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onClickTurnOff();
                            //xTextView.setText("Pitch down");

                            sensorFusion.stop();
                            led.stop(true);
                            alreadyWorked = true;
                            isSingleClickActivated = false;
                            firstStepDonePitch = false;
                            secondStepDonePitch = false;
                            setMySensors();


                        }

                    });
            }
        }







    }

