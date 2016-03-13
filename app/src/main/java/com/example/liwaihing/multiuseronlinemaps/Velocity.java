package com.example.liwaihing.multiuseronlinemaps;

import android.location.Location;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by WaiHing on 21/2/2016.
 */
public class Velocity {
    public static Velocity instance = null;
    private double finalVelocity;
    private double GPSVelocity, AccelerometerVelocity, previousGPSVelocity, previousAccelerometerVelocity;
    private long newTime, lastTime;
    private double acceleration, accuVelocity;
    private boolean GPSAccuracy = false;
    private int onSensorChangeCounter = 0;
    private float[] acceSensorVals = null;
    private ArrayList<Double> consecutiveAcceleration = new ArrayList<>();
    private double averageConsecutiveAcceleration;
    private static float threshold;

    protected Velocity(){
        finalVelocity = 0;
        GPSVelocity = 0;
        AccelerometerVelocity = 0;
        previousGPSVelocity = 0;
        previousAccelerometerVelocity = 0;
        acceleration = 0;
        accuVelocity = 0;
        averageConsecutiveAcceleration = 0;

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateAcceleration();
            }
        }, 2000, 1000);
    }

    static public Velocity getInstance(){
        if(instance == null){
            instance = new Velocity();
        }
        return instance;
    }

    public void onGPSUpdate(Location l){
        updateGPSVelocity(l.getSpeed());
        onGPSAccuracy(l);
    }

    public void updateGPSVelocity(double v){
        previousGPSVelocity = GPSVelocity;
        GPSVelocity = v;
    }

    public void updateAccelerometerVelocity(double v){
        previousAccelerometerVelocity = AccelerometerVelocity;
        AccelerometerVelocity = v;
    }

    public void onSensorUpdate(long time, float[] vals){
        newTime = time;
        acceleration = Math.sqrt(Math.pow(vals[0], 2) + Math.pow(vals[1], 2));
        onConsecutiveAcceleration(vals);
        if (newTime > lastTime){
            accuVelocity += acceleration*(newTime-lastTime);
        }
    }

    private void updateAcceleration(){
        if(accuVelocity != 0){
            onAverageVelocity((newTime - lastTime));
        }
        lastTime = newTime;
        accuVelocity = 0;
    }

    private void onConsecutiveAcceleration(float[] vals){
        if(acceSensorVals != null){
            double v1 = vals[0] * acceSensorVals[0] + vals[1] * acceSensorVals[1] + vals[2] * acceSensorVals[2];
            double v2 = Math.sqrt(Math.pow(vals[0], 2) + Math.pow(vals[1], 2) + Math.pow(vals[2], 2));
            double v3 = Math.sqrt(Math.pow(acceSensorVals[0], 2) + Math.pow(acceSensorVals[1], 2) + Math.pow(acceSensorVals[2], 2));
            double d = v1/(v2*v3);
            if(consecutiveAcceleration.size()>=10){
                double average = 0;
                double counter = 0;
                for(int i=0;i<consecutiveAcceleration.size();i++){
                    average += (i+1) * consecutiveAcceleration.get(i);
                    counter += (i+1);
                }
                averageConsecutiveAcceleration = average/counter;
                consecutiveAcceleration.remove(0);
            }else{
                consecutiveAcceleration.add(d);
            }
        }
        acceSensorVals = vals;
    }

    private String getAccelerometerActivity(){
        String activity = "";
        if(averageConsecutiveAcceleration>0.9 && averageConsecutiveAcceleration<1){
            activity = "walking";
        }else{
            activity = "others";
        }
        return activity;
    }

    private void onAverageVelocity(double timePassed) {
        double accumulatedVelocity = (accuVelocity/timePassed);
        updateAccelerometerVelocity(accumulatedVelocity);
    }

    public void onGPSAccuracy(Location l){
        if (l.getAccuracy()>30 || l.getSpeed()==0){
            GPSAccuracy = false;
        }else{
            GPSAccuracy = true;
        }
    }

    public double getFinalVelocity(){
        if(!GPSAccuracy){
            finalVelocity = getAccelerometerVelocity();
        }else {
            finalVelocity = getGPSVelocity();
        }
        return finalVelocity;
    }

    public double getGPSVelocity() {
        return GPSVelocity;
    }

    public double getAccelerometerVelocity() {
        return AccelerometerVelocity;
    }

}
