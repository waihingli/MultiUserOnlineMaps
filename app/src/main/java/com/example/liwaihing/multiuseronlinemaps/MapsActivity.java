package com.example.liwaihing.multiuseronlinemaps;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.os.AsyncTask;
import android.support.multidex.MultiDex;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
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
    private TextView tv_Distance, tv_Duration;
    private LinearLayout layout_pos, layout_addSharing;
    private double distance = 0;
    private ArrayList<LatLng> markerPoints;
    private ArrayList<UserPosition> userPositionList;
    private Polyline polyline = null;
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private DrawerListAdapter listAdapter;
    private double userVelocity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        myBroadcastReceiver = new MyBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.LOCATION_SERVICE);
        intentFilter.addAction(Constants.SENSOR_SERVICE);
        this.registerReceiver(myBroadcastReceiver, intentFilter);
        Firebase.setAndroidContext(this);
        dbHelper = new DatabaseHelper(this);
        dbHelper.stopSharing();
        velocity = Velocity.getInstance();
        setUpMapIfNeeded();
        setUpDrawerLayout();
        setUpListener();
        btn_menu = (ImageButton) findViewById(R.id.btn_menu);
        btn_share = (ImageButton) findViewById(R.id.btn_share);
        btn_menu.setOnClickListener(onClickMenu);
        btn_share.setOnClickListener(onClickShare);
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
        mMap.setOnMarkerClickListener(onClickMarker);
    }

    private void setUpDrawerLayout(){
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        drawerList = (ListView) findViewById(R.id.navList);
        listAdapter = new DrawerListAdapter(this, CommonUserList.getSharingProfileList());
        drawerList.setAdapter(listAdapter);
        drawerList.setOnItemClickListener(onListItemClickListener);
        layout_addSharing = (LinearLayout) findViewById(R.id.addSharing);
        layout_addSharing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(ShareActivity.class);
                drawerLayout.closeDrawers();
            }
        });
        TextView tv_name = (TextView) findViewById(R.id.userName);
        TextView tv_googleid = (TextView) findViewById(R.id.googleID);
        ImageView img_pic = (ImageView) findViewById(R.id.profilePicture);
        tv_name.setText(dbHelper.getDisplayname());
        tv_googleid.setText(dbHelper.getGoogleID() + "@gmail.com");
        img_pic.setImageBitmap(dbHelper.getProfilePicture());
        this.registerForContextMenu(drawerList);
    }

    private void setUpListener(){
        Firebase sharingRef = dbHelper.getUserSharingPath(dbHelper.getGoogleID());
        sharingRef.addChildEventListener(new ChildEventListener() {
            @Override
            public synchronized void onChildAdded(DataSnapshot dataSnapshot, String s) {
                String user = dataSnapshot.getValue(String.class);
                boolean isUserSharing = false;
                for(String name : CommonUserList.getUserSharingList()){
                    if (name.equals(user)){
                        isUserSharing = true;
                        break;
                    }
                }

                if(!isUserSharing){
                    CommonUserList.addUserSharingList(user);
                    double lat = 0, lon = 0, v = 0;
                    UserPosition userPos = new UserPosition(user, lat, lon, v);
                    userPositionList.add(userPos);
                    for(UserProfile up : CommonUserList.getUserProfileList()){
                        if(up.getUserProfile(user)!=null){
                            up.setIsSharing(true);
                            CommonUserList.addSharingProfileList(up);
                            break;
                        }
                    }
                    Firebase updatePosRef = dbHelper.getUserPositionPath(userPos.getUsername());
                    updatePosRef.addValueEventListener(onSharingPosUpdateListener);
                    listAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public synchronized void onChildRemoved(DataSnapshot dataSnapshot) {
                String name = dataSnapshot.getValue(String.class);
                for(UserPosition u : userPositionList){
                    if(u.getUserPosition(name)!=null){
                        if(u.getMarkers().size()>0){
                            Marker m = u.getMarkers().get(0);
                            m.remove();
                        }
                        layout_pos.setVisibility(View.GONE);
                        if(polyline!=null){
                            polyline.remove();
                        }
                        for(UserProfile up : CommonUserList.getUserProfileList()){
                            if(up.getUserProfile(name)!=null){
                                up.setIsSharing(false);
                                CommonUserList.removeSharingProfileList(up);
                                break;
                            }
                        }
                        userPositionList.remove(u);
                        CommonUserList.removeUserSharingList(u.getUsername());
                        Firebase updatePosRef = dbHelper.getUserPositionPath(name);
                        updatePosRef.removeEventListener(onSharingPosUpdateListener);
                        listAdapter.notifyDataSetChanged();
                        break;
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
            for(UserPosition u : userPositionList){
                String name = dataSnapshot.child("User").getValue(String.class);
                if(u.getUserPosition(name)!=null){
                    if(u.getMarkers().size()>0){
                        Marker old = u.getMarkers().get(0);
                        old.remove();
                        u.setMarkers(new ArrayList<Marker>());
                    }
                    u.setLatitude((double) dataSnapshot.child("Latitude").getValue());
                    u.setLongitude((double) dataSnapshot.child("Longitude").getValue());
                    u.setVelocity((double) dataSnapshot.child("Velocity").getValue());
                    u.setActivity(dataSnapshot.child("Activity").getValue(String.class));

                    UserProfile uPro = null;
                    for (UserProfile p : CommonUserList.getUserProfileList()){
                        if(p.getUserProfile(u.getUsername())!=null)
                            uPro = p;
                    }
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
                    u.addMarkers(m);
                }
            }
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {

        }
    };

    private ChildEventListener onInviteListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            final String user = dataSnapshot.getKey();
            Firebase ref = dbHelper.getUserProfilePath(user);
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    final UserProfile userPro = new UserProfile(user);
                    userPro.setDisplayName(dataSnapshot.child("Name").getValue(String.class));
                    userPro.setProfilePic(dataSnapshot.child("Picture").getValue(String.class));

                    String msg = userPro.getDisplayName() + " invited you to share location.";
                    AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                    builder.setMessage(msg)
                            .setCancelable(false)
                            .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                                public void onClick(@SuppressWarnings("unused") DialogInterface dialog, @SuppressWarnings("unused") int id) {
                                    userPro.setIsSharing(true);
                                    boolean exist = false;
                                    for(UserProfile up : CommonUserList.getUserProfileList()){
                                        if(up.getUserProfile(userPro.getGoogleID())!=null){
                                            exist = true;
                                            break;
                                        }
                                    }
                                    if(!exist){
                                        CommonUserList.addUserProfileList(userPro);
                                        CommonUserList.addShareList(userPro.getGoogleID());
                                    }
                                    dbHelper.addSharingUser(userPro.getGoogleID(), dbHelper.getGoogleID());
                                    dbHelper.addSharingUser(dbHelper.getGoogleID(), userPro.getGoogleID());
                                    dbHelper.removeInvitation(userPro.getGoogleID());
                                    dbHelper.addShareList(userPro.getGoogleID());
                                }
                            })
                            .setNegativeButton("Reject", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, @SuppressWarnings("unused") int id) {
                                    dbHelper.removeInvitation(userPro.getGoogleID());
                                    dialog.cancel();
                                }
                            });
                    final AlertDialog alert = builder.create();
                    alert.show();
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {

                }
            });
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

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
            TextView tv_user = (TextView) findViewById(R.id.tv_user);
            ImageView img_userPic = (ImageView) findViewById(R.id.img_profilePic);
            ImageView img_activity = (ImageView) findViewById(R.id.img_activity);
            String user = marker.getTitle();
            String activity = "";
            for(UserPosition u : userPositionList){
                if(u.getUserPosition(user)!=null){
                    userVelocity = u.getVelocity();
                    activity = u.getActivity();
                }
            }
            UserProfile userPro = null;
            for(UserProfile u : CommonUserList.getSharingProfileList()){
                if(u.getUserProfile(user)!=null){
                    userPro = u;
                }
            }
            img_userPic.setImageBitmap(userPro.getProfilePic());
            tv_user.setText(userPro.getDisplayName());
            if(activity.toUpperCase().equals("WALKING")){
                img_activity.setImageResource(R.drawable.walk_icon);
            }else if(activity.toUpperCase().equals("VEHICLE")){
                img_activity.setImageResource(R.drawable.vehicle_icon);
            }
            LatLng userLatLng = marker.getPosition();
            if(currentLocation!=null){
                markerPoints.add(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
                markerPoints.add(userLatLng);
            }

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

    private AdapterView.OnItemClickListener onListItemClickListener = new AdapterView.OnItemClickListener(){
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            double lat = 0;
            double lon = 0;
            String user = CommonUserList.getUserSharingList().get(position);
            for (UserPosition u : userPositionList){
                if(u.getUserPosition(user)!=null){
                    lat = u.getLatitude();
                    lon = u.getLongitude();
                    break;
                }
            }
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 16));
            drawerLayout.closeDrawers();
        }
    };

    private ImageButton.OnClickListener onClickMenu = new ImageButton.OnClickListener(){
        @Override
        public void onClick(View v) {
            drawerLayout.openDrawer(Gravity.LEFT);
        }
    };

    private ImageButton.OnClickListener onClickShare = new ImageButton.OnClickListener(){
        @Override
        public void onClick(View v) {
            startActivity(ShareActivity.class);
        }
    };

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.drawer_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int position = info.position;
        String user = CommonUserList.getUserSharingList().get(position);
        if (item.getItemId() == R.id.action_stopShare) {
            for(UserPosition u : userPositionList){
                if(u.getUserPosition(user)!=null){
                    Marker m = u.getMarkers().get(0);
                    m.remove();
                    break;
                }
            }
            dbHelper.removeSharingUser(dbHelper.getGoogleID(), user);
            dbHelper.removeSharingUser(user, dbHelper.getGoogleID());
            layout_pos.setVisibility(View.GONE);
            if(polyline!=null){
                polyline.remove();
            }
            drawerLayout.closeDrawers();
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        layout_pos.setVisibility(View.GONE);
        if(polyline!=null){
            polyline.remove();
        }
        Firebase inviteRef = dbHelper.getUserInvitationPath(dbHelper.getGoogleID());
        inviteRef.addChildEventListener(onInviteListener);
    }

    @Override
    protected void onDestroy() {
        stopService(LocationService.class);
        stopService(SensorService.class);
        this.unregisterReceiver(myBroadcastReceiver);
        dbHelper.removeAllRequest();
        dbHelper.stopSharing();
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        Firebase inviteRef = dbHelper.getUserInvitationPath(dbHelper.getGoogleID());
        inviteRef.removeEventListener(onInviteListener);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        final Dialog dialog = new Dialog(MapsActivity.this, R.style.FullHeightDialog);
        dialog.setContentView(R.layout.layout_alert);
        Button btn_leave = (Button) dialog.findViewById(R.id.btn_alertleave);
        btn_leave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                finish();
            }
        });
        dialog.show();
        return;
    }


    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        MultiDex.install(this);
    }

    private void stopService(Class c){
        Intent i = new Intent(this, c);
        stopService(i);
    }

    private void setUpMapIfNeeded() {
        if (mMap == null) {
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        mMap.setMyLocationEnabled(true);
        if(currentLocation != null){
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), 16));
        } else {
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
        if(speed<1){
            speed = 1.4;
        }
        double eDuration = 0;
        eDuration = distance/(speed+userVelocity);
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
        String mode = "mode=walking";
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + mode;
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

            if(intent.getAction().equals(Constants.LOCATION_SERVICE)){
                if(currentLocation == null){
                    currentLocation = (Location) bundle.get(Constants.LOCATION_LOCATION);
                    setUpMap();
                }
                currentLocation = (Location) bundle.get(Constants.LOCATION_LOCATION);
                velocity.onGPSUpdate(currentLocation);
            }
            if(intent.getAction().equals(Constants.SENSOR_SERVICE)){
                long time = bundle.getLong(Constants.SENSOR_TIME);
                float[] vals = bundle.getFloatArray(Constants.SENSOR_ACCEVALS);
                velocity.onSensorUpdate(time, vals);
            }
            if(currentLocation!=null) {
                dbHelper.updatePosition(currentLocation, velocity.getFinalVelocity(), velocity.getActivity());
            }
        }
    }

    public class DrawerListAdapter extends BaseAdapter{
        ArrayList<UserProfile> data;
        LayoutInflater layoutInflater;

        class ViewHolder{
            ImageView userPic;
            TextView googleId;
        }

        public DrawerListAdapter(Context context, ArrayList<UserProfile> data){
            this.data = data;
            layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if(convertView==null){
                convertView=layoutInflater.inflate(R.layout.layout_draweritem, null);
                holder = new ViewHolder();
                holder.userPic = (ImageView) convertView.findViewById(R.id.img_profilePic);
                holder.googleId = (TextView) convertView.findViewById(R.id.tv_googleid);
                convertView.setTag(holder);
            }else{
                holder = (ViewHolder) convertView.getTag();
            }
            UserProfile userPro = data.get(position);
            holder.userPic.setImageBitmap(userPro.getProfilePic());
            holder.googleId.setText(userPro.getDisplayName());
            return convertView;
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

    private class DirectionsJSONParser {
        private JSONObject disTimeOb;
        private JSONObject distanceOb;

        public List<List<HashMap<String,String>>> parse(JSONObject jObject){
            List<List<HashMap<String, String>>> routes = new ArrayList<>() ;
            JSONArray routeArray, legArray, stepArray;

            try {
                routeArray = jObject.getJSONArray("routes");
                for(int i=0;i<routeArray.length();i++){
                    legArray = ( (JSONObject)routeArray.get(i)).getJSONArray("legs");
                    disTimeOb = legArray.getJSONObject(0);
                    distanceOb = disTimeOb.getJSONObject("distance");
                    List path = new ArrayList<>();
                    for(int j=0;j<legArray.length();j++){
                        stepArray = ( (JSONObject)legArray.get(j)).getJSONArray("steps");
                        for(int k=0;k<stepArray.length();k++){
                            String polyline = "";
                            polyline = (String)((JSONObject)((JSONObject)stepArray.get(k)).get("polyline")).get("points");
                            List<LatLng> list = decodePoly(polyline);
                            for(int l=0;l<list.size();l++){
                                HashMap<String, String> hm = new HashMap<>();
                                hm.put("lat", Double.toString((list.get(l)).latitude));
                                hm.put("lng", Double.toString((list.get(l)).longitude));
                                path.add(hm);
                            }
                        }
                        routes.add(path);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }catch (Exception e){
            }

            return routes;
        }

        public String getDistanceMeters() {
            String s = "";
            try {
                s = distanceOb.getString("value");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return s;
        }

        private List<LatLng> decodePoly(String encoded) {
            List<LatLng> poly = new ArrayList<>();
            int index = 0, len = encoded.length();
            int lat = 0, lng = 0;

            while (index < len) {
                int b, shift = 0, result = 0;
                do{
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                }while (b >= 0x20);
                int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lat += dlat;

                shift = 0;
                result = 0;
                do{
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                }while (b >= 0x20);
                int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lng += dlng;

                LatLng p = new LatLng((((double) lat / 1E5)),
                        (((double) lng / 1E5)));
                poly.add(p);
            }

            return poly;
        }
    }
}
