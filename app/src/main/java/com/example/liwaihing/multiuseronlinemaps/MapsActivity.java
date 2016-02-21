package com.example.liwaihing.multiuseronlinemaps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.FileDescriptor;
import java.io.PrintWriter;

public class MapsActivity extends FragmentActivity {
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Location currentLocation = null;
    private Velocity velocity;
    private MyBroadcastReceiver myBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        myBroadcastReceiver = new MyBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Params.LOCATION_SERVICE);
        intentFilter.addAction(Params.SENSOR_SERVICE);
        this.registerReceiver(myBroadcastReceiver, intentFilter);
        velocity = Velocity.getInstance();
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    @Override
    protected void onDestroy() {
        stopService(LocationService.class);
        stopService(SensorService.class);
        this.unregisterReceiver(myBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void onStopService(View v){
        Intent i = new Intent(this, LocationService.class);
        Intent j = new Intent(this, SensorService.class);
        stopService(i);
        stopService(j);
    }

    private void stopService(Class c){
        Intent i = new Intent(this, c);
        stopService(i);
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        mMap.setMyLocationEnabled(true);
        if(currentLocation != null){
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), 16));
        }else{
            Toast.makeText(this, "Location not find", Toast.LENGTH_SHORT);
        }
    }

    public class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if(intent.getAction().equals(Params.LOCATION_SERVICE)){
                if(currentLocation == null){
                    currentLocation = (Location) bundle.get("location");
                    setUpMap();
                }
                currentLocation = (Location) bundle.get("location");
                velocity.updateGPSVelocity(currentLocation.getSpeed());
            }
            if(intent.getAction().equals(Params.SENSOR_SERVICE)){
                double sensorVelocity = bundle.getDouble("sensorVelocity");
                velocity.updateAccelerometerVelocity(sensorVelocity);
            }
        }
    }
}
