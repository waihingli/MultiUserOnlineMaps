package com.example.liwaihing.multiuseronlinemaps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.support.multidex.MultiDex;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity {
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Location currentLocation = null;
    private Velocity velocity;
    private DatabaseHelper dbHelper;
    private MyBroadcastReceiver myBroadcastReceiver;
    private ImageButton btn_menu, btn_share;
    private TextView tv_GPS, tv_Sensor, tv_Distance, tv_Duration;
    private double distance = 0;
    private static final LatLng destination = new LatLng(22.441052, 114.032718);
    private ArrayList<LatLng> markerPoints;

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
        dbHelper = DatabaseHelper.getInstance(this);
        setUpMapIfNeeded();
        btn_menu = (ImageButton) findViewById(R.id.btn_menu);
        btn_share = (ImageButton) findViewById(R.id.btn_share);
        btn_menu.setOnClickListener(onClickMenu);
        btn_share.setOnClickListener(onClickShare);
        tv_GPS = (TextView) findViewById(R.id.tv_GPSV);
        tv_Sensor = (TextView) findViewById(R.id.tv_SensorV);
        tv_Distance = (TextView) findViewById(R.id.tv_distance);
        tv_Duration = (TextView) findViewById(R.id.tv_duration);
        markerPoints = new ArrayList<>();
    }

    private ImageButton.OnClickListener onClickMenu = new ImageButton.OnClickListener(){
        @Override
        public void onClick(View v) {

        }
    };

    private ImageButton.OnClickListener onClickShare = new ImageButton.OnClickListener(){
        @Override
        public void onClick(View v) {
            startActivity(ShareActivity.class);
        }
    };

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

    public void onNavigation(View v){
        markerPoints.add(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
        markerPoints.add(destination);
        MarkerOptions options = new MarkerOptions();
        options.position(destination);
        options.icon(BitmapDescriptorFactory
                .defaultMarker(BitmapDescriptorFactory.HUE_RED));
        mMap.addMarker(options);

        if (markerPoints.size() >= 2) {
            LatLng origin = markerPoints.get(0);
            LatLng dest = markerPoints.get(1);
            String url = getDirectionsUrl(origin, dest);
            DownloadTask downloadTask = new DownloadTask();
            downloadTask.execute(url);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(dest, 16));
        }
    }

    public String distanceUnitConverter(double dist){
        String s = "";
        if (dist >= 1000){
            s = dist/1000 + " km";
        }else{
            s = (int)Math.ceil(dist) + " m";
        }
        return s;
    }

    public String durationUnitConverter(double dura){
        String s = "";
        if (dura > 3600){
            s = (int)dura/3600 + " hour" + (int)Math.ceil((dura%3600)/60) + " min";
        }else{
            s = (int)Math.ceil(dura/60) + " min";
        }
        return s;
    }

    public void distanceParser(String dist){
        distance = Double.parseDouble(dist);
        tv_Distance.setText(distanceUnitConverter(distance));
        tv_Duration.setText(durationUnitConverter(estimatedDuration()));
    }

    public double estimatedDuration(){
        double speed = velocity.getFinalVelocity();
        double eDuration = 0;
        if(speed < 1){
            eDuration = distance/1.4;
        }else{
            eDuration = distance/speed;
        }
        return eDuration;
    }

    private void startActivity(Class c){
        Intent i = new Intent(this, c);
        startActivity(i);
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        String sensor = "sensor=false";
        String parameters = str_origin + "&" + str_dest + "&" + sensor;
        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;

        return url;
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream inputStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();
            inputStream = urlConnection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuffer stringBuffer = new StringBuffer();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                stringBuffer.append(line);
            }
            data = stringBuffer.toString();
            bufferedReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            inputStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    public class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            DecimalFormat df = new DecimalFormat("#.###");
            if(intent.getAction().equals(Params.LOCATION_SERVICE)){
                if(currentLocation == null){
                    currentLocation = (Location) bundle.get("location");
                    setUpMap();
                }
                currentLocation = (Location) bundle.get("location");
                velocity.onGPSUpdate(currentLocation);
                tv_GPS.setText(df.format(velocity.getGPSVelocity()*100) + " cm/s");
            }
            if(intent.getAction().equals(Params.SENSOR_SERVICE)){
                double acceleration = bundle.getDouble("acceleration");
                long time = bundle.getLong("time");
                velocity.onSensorUpdate(acceleration, time);
                tv_Sensor.setText(df.format(velocity.getAccelerometerVelocity()*100) + " cm/s");
            }
            if(currentLocation!=null) {
                dbHelper.updatePosition(currentLocation, velocity.getFinalVelocity());
            }
        }
    }

    private class DownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {
            String data = "";
            try {
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            ParserTask parserTask = new ParserTask();
            parserTask.execute(result);
        }
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {
        private String dist;
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {
            JSONObject jsonObject;
            List<List<HashMap<String, String>>> routes = null;
            try {
                jsonObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser jsonParser = new DirectionsJSONParser();
                routes = jsonParser.parse(jsonObject);
                dist = jsonParser.getDistanceMeters();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;

            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();
                List<HashMap<String, String>> path = result.get(i);
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);
                    points.add(position);
                }
                lineOptions.addAll(points);
                lineOptions.width(12);
                lineOptions.color(Color.GRAY);
            }
            distanceParser(dist);
            mMap.addPolyline(lineOptions);
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        MultiDex.install(this);
    }
}
