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
    private double GPSVelocity, AccelerometerVelocity, previousGPSVelocity, previousAccelerometerVelocity;
    private long newTime, lastTime;
    private double acceleration, accuVelocity;
    private boolean GPSAccuracy = false;
    private int onSensorChangeCounter = 0;

    protected Velocity(){
        finalVelocity = 0;
        GPSVelocity = 0;
        AccelerometerVelocity = 0;
        previousGPSVelocity = 0;
        previousAccelerometerVelocity = 0;
        acceleration = 0;
        accuVelocity = 0;

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

    public void onSensorUpdate(double a, long time){
        acceleration = a;
        newTime = time;
        if (newTime > lastTime){
            accuVelocity += acceleration*(newTime-lastTime);
            onSensorChangeCounter++;
        }
    }

    private void updateAcceleration(){
        if(accuVelocity != 0){
            onAverageVelocity((newTime - lastTime));
        }
        lastTime = newTime;
        accuVelocity = 0;
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
