package com.example.liwaihing.multiuseronlinemaps;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by WaiHing on 21/2/2016.
 */
public class LocationService extends Service implements LocationListener {

    public static final String BROADCAST_ACTION = "LocationService";
    public LocationManager locationManager;
    Intent intent;
    private Location currentLocation = null;
    private boolean findLocation = false;

    public Location getCurrentLocation(){
        return currentLocation;
    }

    public Location getLastLocation(){
        Location lastLocation = locationManager.getLastKnownLocation(locationManager.getBestProvider(new Criteria(), true));
        return lastLocation;
    }

    public LocationManager getLocationManager(){
        return locationManager;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        intent = new Intent(BROADCAST_ACTION);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

        Log.i("success", "Location Service started");

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        findLocation = true;

        Intent i = new Intent();
        i.putExtra("location", location);
        i.setAction("android.intent.action.locationChange");
        sendBroadcast(i);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}
