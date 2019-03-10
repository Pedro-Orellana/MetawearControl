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
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.data.EulerAngles;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.SensorFusionBosch;
import com.mbientlab.metawear.module.Switch;


import bolts.Task;

public class ConnectedActivity extends AppCompatActivity {
    private MetaWearBoard myBoard;
    private TextView connectingTextView;
    private ProgressBar connectingProgressBar;
    private LinearLayout linearLayout;
    private TextView xTextView;
    private TextView yTextView;
    private TextView zTextView;
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
    private Boolean firstStepDoneRoll;
    private Boolean secondStepDonePitch;

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
                        led = myBoard.getModule(Led.class);
                        sensorFusion = myBoard.getModule(SensorFusionBosch.class);
                        pushButton = myBoard.getModule(Switch.class);
                        alreadyWorked = true;
                        setMySensors();
                    }
                },5000);

            }
        }

    }



        public void onClickStop(View view) {
        led.stop(true);
        }

        public void setMySensors(){
        //This is where most of the logic of the app is written.

            firstStepDonePitch = false;
            secondStepDonePitch = false;
            firstStepDoneRoll = false;

            led.editPattern(Led.Color.GREEN, Led.PatternPreset.SOLID).commit();
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
                                        if(!testingRunning) {
                                            testingRunning = true;
                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    readNumberOfClicks();
                                                }
                                            },300);

                                        }
                                        numberOfClicks = numberOfClicks +1;
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
                        if(isSingleClickActivated){
                            doSingleClick(data);
                        } else if (isDoubleClickActivated){
                        }

                    }
                });
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
                            xTextView.setText("Pitch up");
                            sensorFusion.stop();
                            led.stop(true);
                            alreadyWorked = true;
                            isSingleClickActivated = false;


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
                        firstStepDonePitch = false;
                        secondStepDonePitch = false;
                        led.stop(true);
                        sensorFusion.stop();
                        isSingleClickActivated = false;
                        alreadyWorked = true;
                    }
                });

            } else if(!secondStepDonePitch){

                runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            xTextView.setText("Pitch down");
                            sensorFusion.stop();
                            led.stop(true);
                            alreadyWorked = true;
                            isSingleClickActivated = false;
                            firstStepDonePitch = false;
                            secondStepDonePitch = false;

                        }

                    });
            }
        }







    }

