package com.example.liwaihing.multiuseronlinemaps;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;

/**
 * Created by WaiHing on 21/2/2016.
 */
public class SensorService  extends Service implements SensorEventListener {

    public static final String BROADCAST_ACTION = "SensorService";
    public SensorManager sensorManager;
    public Sensor accelerometer;
    Intent intent;
    Filter filter;
    protected float[] acceSensorVals;

    @Override
    public void onCreate() {
        super.onCreate();
        intent = new Intent(BROADCAST_ACTION);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)!=null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        filter = new Filter();
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
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            acceSensorVals = filter.lowPass(event.values.clone(), acceSensorVals);
            Intent i = new Intent();
            i.putExtra(Constants.SENSOR_TIME, System.currentTimeMillis());
            i.putExtra(Constants.SENSOR_ACCEVALS, acceSensorVals);
            i.setAction(Constants.SENSOR_SERVICE);
            sendBroadcast(i);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public class Filter {
        private static final float ALPHA = 0.15f;

        protected Filter(){}

        protected float[] lowPass(float[] input, float[] output){
            if (output == null)
                return input;
            for (int i=0; i<input.length; i++){
                output[i] = output[i] + ALPHA * (input[i] - output[i]);
            }
            return output;
        }
    }
}