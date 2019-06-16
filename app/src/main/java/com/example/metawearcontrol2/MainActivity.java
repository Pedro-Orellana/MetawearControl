package com.example.metawearcontrol2;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
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
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.design.button.MaterialButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.mbientlab.metawear.android.BtleService;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ServiceConnection {

    //physical address of board. Can be discovered through BLE scan if necessary;
    static final String METAWEAR_MAC_ADDRESS = "CB:42:F6:6E:2C:7A";
    private BluetoothDevice foundMetawearDevice;
    static BtleService.LocalBinder myLocalBinder;

    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private MaterialButton connectButton;


    private AnimationModel controlModel;
    private ImageView animationView;
    private AlphaAnimation fadeIn;
    private AlphaAnimation fadeOut;
    private int currentImageInt;
    private Handler handler;
    private BluetoothManager bluetoothManager;


    //This has the onClick for the recyclerView.
    interface onClickImplementation {
        void myOnClick();

    }

    //This is the interface that passes the BluetoothDevice to the ConnectedActivity.

    private onClickImplementation MainActivityClickImplementation = new onClickImplementation() {
        @Override
        public void myOnClick() {
            Intent intent = new Intent(MainActivity.this, ConnectedActivity.class);
            intent.putExtra("BluetoothDevice", foundMetawearDevice);
            startActivity(intent);


        }
    };





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initializing handler;
        handler = new Handler();

        //Finding the toolbar and showing it on screen
        Toolbar toolbar = findViewById(R.id.toolbar_main_activity);
        setSupportActionBar(toolbar);

        //getting the Bluetooth manager to do the scan
        bluetoothManager = (BluetoothManager) this.getSystemService(BLUETOOTH_SERVICE);

        //getting a reference of the views
        progressBar = findViewById(R.id.scanning_progress_bar);
        //progressBar.setVisibility(View.VISIBLE);
        recyclerView = findViewById(R.id.recycler_scan_results);
        connectButton = findViewById(R.id.material_button_connect);

        //setting the animation to the ImageView
        animationView = findViewById(R.id.image_animation_view);
        animationView.setImageResource(R.drawable.good3);
        currentImageInt = R.drawable.good3;


        //initialize the animations and the animation set
        fadeIn = new AlphaAnimation(0.0f,1.0f);
        fadeIn.setDuration(1000);

        fadeOut = new AlphaAnimation(1.0f,0.0f);
        fadeOut.setStartOffset(3000);
        fadeOut.setDuration(1000);


        controlModel =  new ViewModelProvider.AndroidViewModelFactory(getApplication()).create(AnimationModel.class);
        final Observer<Integer> imageObserver = new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer integer) {
                  handler.postDelayed(()->{
                      switch(integer){
                          case 3:
                            animationView.animate()
                                    .setDuration(1000)
                                    .alpha(0f)
                                    .withEndAction(()->{
                                        animationView.setImageResource(R.drawable.good2);
                                        animationView.animate()
                                                .setDuration(1000)
                                                .alpha(1f)
                                                .withEndAction( ()->{
                                                    controlModel.getCurrentImage().setValue(2);
                                                });

                                    });
                            break;


                          case 2:
                              animationView.animate()
                                      .setDuration(1000)
                                      .alpha(0f)
                                      .withEndAction(()->{
                                          animationView.setImageResource(R.drawable.good1);
                                          animationView.animate()
                                                  .setDuration(1000)
                                                  .alpha(1f)
                                                  .withEndAction( ()->{
                                                      controlModel.getCurrentImage().setValue(1);
                                                  });

                                      });
                              break;

                          case 1:
                              animationView.animate()
                                      .setDuration(1000)
                                      .alpha(0f)
                                      .withEndAction(()->{
                                          animationView.setImageResource(R.drawable.good3);
                                          animationView.animate()
                                                  .setDuration(1000)
                                                  .alpha(1f)
                                                  .withEndAction( ()->{
                                                      controlModel.getCurrentImage().setValue(3);
                                                  });

                                      });
                              break;




                      }
                  },3000);
            }
        };

        controlModel.getCurrentImage().observe(this,imageObserver);




        //getting the service and binding it to the activity
        bindService(new Intent(this, BtleService.class), this, Context.BIND_AUTO_CREATE);


        controlModel.getCurrentImage().setValue(3);
    }

    @Override
    protected void onStart() {
        super.onStart();
        animationView.startAnimation(fadeIn);




    }



    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        myLocalBinder = (BtleService.LocalBinder) service;


    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }





    static class AnimationModel extends ViewModel{
        private MutableLiveData<Integer> currentImage;

        public MutableLiveData<Integer> getCurrentImage() {
            if(currentImage == null){
                currentImage = new MutableLiveData<>();
            }
            return currentImage;
        }
    }






    public void onClickStartScan(View x){
        if (bluetoothManager != null) {
            startBluetoothScan(bluetoothManager);
            connectButton.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);


        }
    }



    // function that takes care of the scan of the board.
    private void startBluetoothScan(BluetoothManager manager) {
        BluetoothLeScanner scanner = manager.getAdapter().getBluetoothLeScanner();
        if (scanner == null) {
            //scanningTextView.setText("the scanner is null");
        } else {
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
            if (foundMetawearDevice != null) {
                String deviceName = foundMetawearDevice.getName();
                String deviceAddress = foundMetawearDevice.getAddress();
                int deviceType = foundMetawearDevice.getType();

                RecyclerViewAdapter recyclerViewAdapter = new RecyclerViewAdapter(deviceName, deviceAddress, deviceType, MainActivityClickImplementation);
                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.this);
                recyclerView.setLayoutManager(linearLayoutManager);
                recyclerView.setAdapter(recyclerViewAdapter);
                progressBar.setVisibility(View.INVISIBLE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

}
