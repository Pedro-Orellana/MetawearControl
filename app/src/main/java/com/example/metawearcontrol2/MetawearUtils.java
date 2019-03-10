package com.example.metawearcontrol2;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.util.Log;

import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.data.EulerAngles;
import com.mbientlab.metawear.module.AccelerometerBosch;
import com.mbientlab.metawear.module.SensorFusionBosch;

import java.util.ArrayList;

import java.util.List;

import bolts.Continuation;
import bolts.Task;


public class MetawearUtils {



}
