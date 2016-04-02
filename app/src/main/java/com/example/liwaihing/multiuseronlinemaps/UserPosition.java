package com.example.liwaihing.multiuseronlinemaps;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import java.util.ArrayList;

/**
 * Created by WaiHing on 7/3/2016.
 */
public class UserPosition {
    private String username;
    private double latitude;
    private double longitude;
    private double velocity;
    private String activity;
    private ArrayList<Marker> markers;

    protected UserPosition(String name, double lat, double lon, double v){
        username = name;
        latitude = lat;
        longitude = lon;
        velocity = v;
        activity = "Walking";
        markers = new ArrayList<>();
    }

    public UserPosition getUserPosition(String name){
        if (name.equals(username)){
            return this;
        }
        return null;
    }

    public String getUsername() {
        return username;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getVelocity() {
        return velocity;
    }

    public void setVelocity(double velocity) {
        this.velocity = velocity;
    }

    public LatLng getLatLng(){
        return new LatLng(getLatitude(), getLongitude());
    }

    public void setActivity(String a){
        activity = a;
    }

    public String getActivity(){
        return activity;
    }

    public ArrayList<Marker> getMarkers(){
        return markers;
    }

    public void setMarkers(ArrayList<Marker> m){
        markers = m;
    }

    public void addMarkers(Marker m){
        markers.add(m);
    }
}
