package com.example.liwaihing.multiuseronlinemaps;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by WaiHing on 21/2/2016.
 */
public class SensorService  extends Service implements SensorEventListener {

    public static final String BROADCAST_ACTION = "SensorService";
    public SensorManager sensorManager;
    public Sensor accelerometer;
    Intent intent;
    FilterHelper filterHelper;
    private long newTime, lastTime;
    private double velocity = 0, acceleration = 0, accuVelocity = 0;
    private int onSensorChangeCounter = 0;
    protected float[] acceSensorVals;

    @Override
    public void onCreate() {
        super.onCreate();
        intent = new Intent(BROADCAST_ACTION);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        filterHelper = new FilterHelper();
        lastTime = System.currentTimeMillis()+5000;

        Log.i("Service", "Sensor Service started");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        newTime = System.currentTimeMillis();
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            acceSensorVals = filterHelper.lowPass(event.values.clone(), acceSensorVals);
            acceleration = Math.sqrt(Math.pow(acceSensorVals[0], 2) + Math.pow(acceSensorVals[1], 2));
        }

        Intent i = new Intent();
        i.putExtra("sensorVelocity", velocity);
        i.putExtra("acceleration", acceleration);
        i.putExtra("time", System.currentTimeMillis());
        i.setAction(Params.SENSOR_SERVICE);
        sendBroadcast(i);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}