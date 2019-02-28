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
import com.mbientlab.metawear.builder.filter.ThresholdOutput;
import com.mbientlab.metawear.data.EulerAngles;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.SensorFusionBosch;

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
    private SensorFusionBosch sensorFusion;

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
        ConnectingAsyncTask  myTask = new ConnectingAsyncTask();
        myTask.execute(device);



    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myBoard.tearDown();
        myBoard.disconnectAsync();
    }

    private class ConnectingAsyncTask extends AsyncTask<BluetoothDevice,Void,Task>{

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
         if(task.isFaulted()){
             connectingTextView.setText("The app could not connect to the board :(");
             connectingProgressBar.setVisibility(View.INVISIBLE);
         }else{
             handler.postDelayed(new Runnable() {
                 @Override
                 public void run() {
                     connectingTextView.setText("Board is connected and ready to go");
                     connectingProgressBar.setVisibility(View.INVISIBLE);
                     linearLayout.setVisibility(View.VISIBLE);
                 }
             },1000);

         }
     }
 }

    public void onClickTestBoard(View view){
        sensorFusion = myBoard.getModule(SensorFusionBosch.class);
        sensorFusion.configure()
                .mode(SensorFusionBosch.Mode.NDOF)
                .accRange(SensorFusionBosch.AccRange.AR_16G)
                .gyroRange(SensorFusionBosch.GyroRange.GR_2000DPS)
                .commit();
        fingerSensor(sensorFusion);
    }


    public void fingerSensor(final SensorFusionBosch sensorFusion){

        sensorFusion.eulerAngles().addRouteAsync(new RouteBuilder() {
            Handler handler = new Handler();
            @Override
            public void configure(RouteComponent source) {
                source.stream(new Subscriber() {
                    @Override
                    public void apply(Data data, Object... env) {
                        //zTextView.setText(Float.toString(data.value(EulerAngles.class).heading()));
                        Log.i("MainActivity", "heading = " + data.value(EulerAngles.class));
                        if(data.value(EulerAngles.class).pitch() > 60.000){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    xTextView.setText("Pitch down");


                                }
                            });

                        } else if(data.value(EulerAngles.class).pitch() < -60.000) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    xTextView.setText("Pitch up");

                                }
                            });


                        }
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


    public void onClickStop(View view){
        sensorFusion.stop();

    }
}
