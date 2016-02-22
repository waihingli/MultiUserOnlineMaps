package com.example.liwaihing.multiuseronlinemaps;

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

    public void updateGPSVelocity(double v){
        previousGPSVelocity = GPSVelocity;
        GPSVelocity = v;
    }

    public void updateAccelerometerVelocity(double v){
        previousAccelerometerVelocity = AccelerometerVelocity;
        AccelerometerVelocity = v;
    }

    public void setSensorAcceleration(double a, long time){
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

    public double getGPSVelocity() {
        return GPSVelocity;
    }

    public double getAccelerometerVelocity() {
        return AccelerometerVelocity;
    }

}
