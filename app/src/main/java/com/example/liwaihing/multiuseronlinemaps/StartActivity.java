package com.example.liwaihing.multiuseronlinemaps;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;

import java.util.List;


public class StartActivity extends Activity {
    LocationManager locationManager;
    private boolean onGPSProvider = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if(!isMyServiceRunning(LocationService.class)) {
            startService(LocationService.class);
        }
        if(!isMyServiceRunning(SensorService.class)) {
            startService(SensorService.class);
        }
        permissionCheck();
        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            startUpFinish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            startUpFinish();
        }
    }

    @Override
    protected void onDestroy() {
        stopService(LocationService.class);
        stopService(SensorService.class);
        super.onDestroy();
    }

    private void permissionCheck(){
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            AlertDialog.Builder builder = new AlertDialog.Builder(StartActivity.this);
            builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(@SuppressWarnings("unused") DialogInterface dialog, @SuppressWarnings("unused") int id) {
                            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, @SuppressWarnings("unused") int id) {
                            dialog.cancel();
                        }
                    });
            final AlertDialog alert = builder.create();
            alert.show();
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceInfo = manager.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                //check it is here
                return true;
            }
        }
        return false;
    }

    private void startUpFinish(){
        startActivity(MapsActivity.class);
    }

    private void startActivity(Class c){
        Intent i = new Intent(this, c);
        startActivity(i);
    }

    private void startService(Class c){
        Intent i = new Intent(this, c);
        startService(i);
    }

    private void stopService(Class c){
        Intent i = new Intent(this, c);
        stopService(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_start, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
