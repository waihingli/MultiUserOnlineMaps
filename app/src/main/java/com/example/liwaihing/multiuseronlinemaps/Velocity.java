package com.example.liwaihing.multiuseronlinemaps;

import android.location.Location;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by WaiHing on 21/2/2016.
 */
public class Velocity {
    public static Velocity instance = null;
    private double finalVelocity;
    private double GPSVelocity, AccelerometerVelocity;
    private long newTime, lastTime;
    private double accuAcceleration, accuVelocity;
    private boolean GPSAccuracy = false;
    private int counter;
    private float[] acceSensorVals = null;

    protected Velocity(){
        finalVelocity = 0;
        GPSVelocity = 0;
        AccelerometerVelocity = 0;
        accuAcceleration = 0;
        accuVelocity = 0;
        counter = 0;

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
        GPSVelocity = v;
    }

    public void updateAccelerometerVelocity(double v){
        AccelerometerVelocity = v;
    }

    public void onSensorUpdate(long time, float[] vals){
        newTime = time;
        acceSensorVals = vals;
        double acceleration = 0;
        if(isStep()){
            acceleration = Math.sqrt(Math.pow(vals[0], 2) + Math.pow(vals[1], 2));
        }
        if (newTime > lastTime){
            accuAcceleration += acceleration*(newTime-lastTime);
        }
    }

    private void updateAcceleration(){
        if(accuAcceleration != 0){
            onAverageVelocity((newTime - lastTime));
        }
        lastTime = newTime;
        accuAcceleration = 0;
    }

    public boolean isStep(){
        boolean isStep = false;
        double energy = Math.sqrt(Math.pow(acceSensorVals[0], 2) + Math.pow(acceSensorVals[1], 2) + Math.pow(acceSensorVals[2], 2));
        if(energy<1.2) {
            isStep = true;
        }
        return isStep;
    }

    public String getActivity(){
        String activity = "Walking";
        if(getFinalVelocity()-getAccelerometerVelocity()>5){    //if gps velocity is greater than accelerometer velocity
            activity = "Vehicle";
        }
        return activity;
    }

    private void onAverageVelocity(double timePassed) {
        double a = (accuAcceleration /timePassed);
        accuVelocity += a;
        counter++;
        if(counter>=5){
            double averageV = accuVelocity/counter;             //update average velocity every 5 sec
            updateAccelerometerVelocity(averageV);
            accuVelocity = 0;
            counter = 0;
        }
    }

    public void onGPSAccuracy(Location l){
        if (l.getAccuracy()>30 || l.getSpeed()==0){             //GPS is not reliable
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
