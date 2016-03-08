package com.example.liwaihing.multiuseronlinemaps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.location.Location;
import android.os.AsyncTask;
import android.support.multidex.MultiDex;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
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

public class MapsActivity extends FragmentActivity{
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Location currentLocation = null;
    private Velocity velocity;
    private DatabaseHelper dbHelper;
    private MyBroadcastReceiver myBroadcastReceiver;
    private ImageButton btn_menu, btn_share;
    private TextView tv_GPS, tv_Sensor, tv_Distance, tv_Duration;
    private LinearLayout layout_pos;
    private double distance = 0;
    private ArrayList<LatLng> markerPoints;
    private ArrayList<UserPosition> userPositionList;
    private ArrayList<ArrayList> markerList;
    private Polyline polyline = null;

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
        dbHelper = new DatabaseHelper(this);
        dbHelper.stopSharing();
        setUpMapIfNeeded();
        setUpListener();
        btn_menu = (ImageButton) findViewById(R.id.btn_menu);
        btn_share = (ImageButton) findViewById(R.id.btn_share);
        btn_menu.setOnClickListener(onClickMenu);
        btn_share.setOnClickListener(onClickShare);
        tv_GPS = (TextView) findViewById(R.id.tv_GPSV);
        tv_Sensor = (TextView) findViewById(R.id.tv_SensorV);
        tv_Distance = (TextView) findViewById(R.id.tv_distance);
        tv_Duration = (TextView) findViewById(R.id.tv_duration);
        layout_pos = (LinearLayout) findViewById(R.id.layout_posDetail);
        layout_pos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layout_pos.setVisibility(View.GONE);
                polyline.remove();
            }
        });
        markerPoints = new ArrayList<>();
        userPositionList = new ArrayList<>();
        markerList = new ArrayList();
        mMap.setOnMarkerClickListener(onClickMarker);
    }

    private void setUpListener(){
        Firebase sharingRef = dbHelper.getUserSharingPath(dbHelper.getGoogleID());
        sharingRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                String user = dataSnapshot.getValue(String.class);
                boolean isUserSharing = false;
                for(int i=0;i<CommonUserList.getUserSharingList().size();i++){
                    if(user.equals(CommonUserList.getUserSharingList().get(i))){
                        isUserSharing = true;
                    }
                }
                if(!isUserSharing){
                    CommonUserList.addUseSharingList(user);
                    double lat = 0, lon = 0, v = 0;
                    UserPosition userPos = new UserPosition(user, lat, lon, v);
                    userPositionList.add(userPos);
                    ArrayList<Marker> markers = new ArrayList<>();
                    markerList.add(markers);
                    Firebase updatePosRef = dbHelper.getUserPositionPath(userPos.getUsername());
                    updatePosRef.addValueEventListener(onSharingPosUpdateListener);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                String name = dataSnapshot.getValue(String.class);
                for(int i=0; i< userPositionList.size(); i++){
                    UserPosition u = userPositionList.get(i);
                    if(u.getUsername().equals(dataSnapshot.child("User").getValue())) {
                        if (markerList.get(i).size() > 0) {
                            Marker m = (Marker) markerList.get(i).get(0);
                            m.remove();
                            markerList.remove(i);
                        }
                        userPositionList.remove(u);
                        Firebase updatePosRef = dbHelper.getUserPositionPath(name);
                        updatePosRef.removeEventListener(onSharingPosUpdateListener);
                    }
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

    }

    private ValueEventListener onSharingPosUpdateListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            for(int i=0; i< userPositionList.size(); i++){
                UserPosition u = userPositionList.get(i);
                if(u.getUsername().equals(dataSnapshot.child("User").getValue())){
                    UserProfile uPro = null;
                    for (UserProfile p : CommonUserList.getUserProfileList()){
                        if(p.getUserProfile(u.getUsername())!=null)
                            uPro = p;
                    }
                    if(markerList.get(i).size()>0){
                        Marker old = (Marker)markerList.get(i).get(0);
                        old.remove();
                        markerList.get(i).clear();
                    }
                    u.setLatitude((double) dataSnapshot.child("Latitude").getValue());
                    u.setLongitude((double) dataSnapshot.child("Longitude").getValue());
                    u.setVelocity((double) dataSnapshot.child("Velocity").getValue());

                    Bitmap proPic = uPro.getProfilePic();
                    Bitmap bmp = Bitmap.createScaledBitmap(proPic, 100, 100, false);
                    Canvas canvas = new Canvas(bmp);
                    Paint paint = new Paint();
                    paint.setColor(Color.BLACK);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(10);
                    canvas.drawBitmap(bmp, 0, 0, paint);
                    canvas.drawRect(0, 0, 100, 100, paint);
                    MarkerOptions options = new MarkerOptions();
                    options.icon(BitmapDescriptorFactory.fromBitmap(bmp))
                            .anchor(0.5f, 1);
                    options.position(u.getLatLng());
                    options.title(u.getUsername());

                    Marker m = mMap.addMarker(options);
                    m.setPosition(u.getLatLng());
                    markerList.get(i).add(m);
                }
            }
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {

        }
    };

    private GoogleMap.OnMarkerClickListener onClickMarker = new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(final Marker marker) {
            layout_pos.setVisibility(View.GONE);
            if(polyline!=null){
                polyline.remove();
            }
            final String name = marker.getTitle();
            UserPosition userPos = null;
            for (UserPosition u : userPositionList){
                if (u.getUserPosition(name)!=null){
                    userPos = u;
                }
            }
            markerPoints.add(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
            markerPoints.add(userPos.getLatLng());
            Button btn_stopShare = (Button) findViewById(R.id.btn_stopShare);
            btn_stopShare.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for(int i=0; i< userPositionList.size(); i++) {
                        UserPosition u = userPositionList.get(i);
                        if(u.getUsername().equals(name)){
                            markerList.remove(i);
                            userPositionList.remove(i);
                        }
                    }
                    CommonUserList.removeUserSharingList(name);
                    for(UserProfile u : CommonUserList.getUserProfileList()){
                        if(u.getUserProfile(name)!=null){
                            u.setIsSharing(false);
                        }
                    }
                    dbHelper.updateSharing(CommonUserList.getUserSharingList());
                    marker.remove();
                    layout_pos.setVisibility(View.GONE);
                    if(polyline!=null){
                        polyline.remove();
                    }
                }
            });

            if (markerPoints.size() >= 2) {
                LatLng origin = markerPoints.get(0);
                LatLng dest = markerPoints.get(1);
                String url = getDirectionsUrl(origin, dest);
                DownloadTask downloadTask = new DownloadTask();
                downloadTask.execute(url);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(dest, 16));
                markerPoints.clear();
            }
            layout_pos.setVisibility(View.VISIBLE);
            return true;
        }
    };

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
        layout_pos.setVisibility(View.GONE);
        if(polyline!=null){
            polyline.remove();
        }
    }

    @Override
    protected void onDestroy() {
        stopService(LocationService.class);
        stopService(SensorService.class);
        this.unregisterReceiver(myBroadcastReceiver);
        dbHelper.stopSharing();
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
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
            polyline = mMap.addPolyline(lineOptions);
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        MultiDex.install(this);
    }
}
