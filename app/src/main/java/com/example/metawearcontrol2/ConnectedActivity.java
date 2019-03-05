package com.example.metawearcontrol2;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.design.button.MaterialButton;
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
import com.mbientlab.metawear.data.SensorOrientation;
import com.mbientlab.metawear.module.AccelerometerBosch;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.SensorFusionBosch;
import com.mbientlab.metawear.module.Switch;


import bolts.Continuation;
import bolts.Task;

public class ConnectedActivity extends AppCompatActivity {
    private MetaWearBoard myBoard;
    private TextView connectingTextView;
    private ProgressBar connectingProgressBar;
    private MaterialButton connectingTestingButton;
    private LinearLayout linearLayout;
    private TextView xTextView;
    private TextView yTextView;
    private TextView zTextView;
    private AccelerometerBosch accBosch;
    private Led led;
    private SensorFusionBosch sensorFusion;
    private Switch pushButton;
    private Boolean isButtonActivated;
    private String buttonPressed;
    private Handler handler;
    private Boolean alreadyWorked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected);
        connectingProgressBar = findViewById(R.id.connecting_progress_bar);
        connectingTestingButton = findViewById(R.id.testing_button);
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


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myBoard.tearDown();
        myBoard.disconnectAsync();
    }

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
                        accBosch = myBoard.getModule(AccelerometerBosch.class);
                        led = myBoard.getModule(Led.class);
                        sensorFusion = myBoard.getModule(SensorFusionBosch.class);
                        pushButton = myBoard.getModule(Switch.class);
                        alreadyWorked = false;
                        setMySensors();
                    }
                },5000);

            }
        }

    }
        public void onClickTestBoard(View view) {




        }


        public void onClickStop(View view) {
        led.stop(true);
       // accBosch.tap().stop();
        //accBosch.stop();
        myBoard.tearDown();



        }

        public void setMySensors(){
            led.editPattern(Led.Color.GREEN, Led.PatternPreset.SOLID).commit();
            isButtonActivated = false;
            sensorFusion.eulerAngles().start();


            pushButton.state().addRouteAsync(new RouteBuilder() {
                @Override
                public void configure(RouteComponent source) {
                    source.stream(new Subscriber() {
                        @Override public void apply(Data data, Object... env) {
                            Log.i("MainActivity",data.types().toString());
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    buttonPressed = data.toString().substring(47,48);
                                    if(buttonPressed.equals("1")){
                                        led.editPattern(Led.Color.GREEN, Led.PatternPreset.SOLID).commit();
                                        led.play();
                                        sensorFusion.start();


                                    }

                                }
                            });

                        }
                    });
                }
            });

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
                        //zTextView.setText(Float.toString(data.value(EulerAngles.class).heading()));
                        Log.i("MainActivity", "heading = " + data.value(EulerAngles.class));
                        if(data.value(EulerAngles.class).pitch() > 60.000){
                            if(!alreadyWorked) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        xTextView.setText("Pitch down");
                                        sensorFusion.stop();
                                        led.stop(true);
                                        alreadyWorked = true;
                                    }
                                });
                        }

                        }else if(alreadyWorked && data.value(EulerAngles.class).pitch() < 60.000 && data.value(EulerAngles.class).pitch() > -60.000 ) {//there is probably nothing I can do here...
                            alreadyWorked = false;
                        } else if(data.value(EulerAngles.class).pitch() < -60.000) {
                            if(!alreadyWorked) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        xTextView.setText("Pitch up");
                                        sensorFusion.stop();
                                        led.stop(true);
                                        alreadyWorked = true;


                                    }
                                });

                            }else if(alreadyWorked && data.value(EulerAngles.class).pitch() > -60.000){

                            }
                        }
                    }
                });
            }
        });

        }



        public void onClickSyncSensor(View view){
        myBoard.tearDown();
        accBosch = myBoard.getModule(AccelerometerBosch.class);
            accBosch.orientation().addRouteAsync(new RouteBuilder() {
                @Override
                public void configure(RouteComponent source) {
                    source.stream(new Subscriber() {
                        @Override
                        public void apply(Data data, Object... env) {
                            Log.i("MainActivity", "Orientation = " + data.value(SensorOrientation.class));
                        }
                    });
                }
            }).continueWith(new Continuation<Route, Void>() {
                @Override
                public Void then(Task<Route> task) throws Exception {
                    accBosch.orientation().start();
                    accBosch.start();
                    return null;
                }
            });
        }
    }

