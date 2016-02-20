package com.example.liwaihing.multiuseronlinemaps;

/**
 * Created by WaiHing on 21/2/2016.
 */
public class Velocity {
    public static Velocity instance = null;
    private double finalVelocity;
    private double GPSVelocity;
    private double previousGPSVelocity;
    private double AccelerometerVelocity;
    private double previousAccelerometerVelocity;

    protected Velocity(){
        finalVelocity = 0;
        GPSVelocity = 0;
        previousGPSVelocity = 0;
        AccelerometerVelocity = 0;
        previousAccelerometerVelocity = 0;
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
}
