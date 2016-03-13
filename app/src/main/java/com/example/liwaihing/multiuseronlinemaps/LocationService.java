package com.example.liwaihing.multiuseronlinemaps;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by WaiHing on 21/2/2016.
 */
public class LocationService extends Service implements LocationListener {

    public static final String BROADCAST_ACTION = "LocationService";
    public static LocationManager locationManager;
    Intent intent;

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

        Log.i("Service", "Location Service started");

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        locationManager.removeUpdates(this);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        Intent i = new Intent();
        i.putExtra(Constants.LOCATION_LOCATION, location);
        i.setAction(Constants.LOCATION_SERVICE);
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
