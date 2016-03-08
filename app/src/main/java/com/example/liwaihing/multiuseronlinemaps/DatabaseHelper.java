package com.example.liwaihing.multiuseronlinemaps;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Created by WaiHing on 5/3/2016.
 */
public class DatabaseHelper {
    public static final String PATH = "https://multiuseronlinemap.firebaseio.com/";
    private Firebase myFireBaseRef = new Firebase(PATH);
    private SharedPreferences settings;
    private String username;
    ArrayList<String> shareList;

    protected DatabaseHelper(Context context){
        settings = context.getSharedPreferences("user_auth", Context.MODE_PRIVATE);
        username = settings.getString("username", "");
        shareList = new ArrayList<>();
    }

    public void updatePosition(Location l, double v){
        Firebase positionRef = getUserPositionPath(username);
        positionRef.child("User").setValue(username);
        positionRef.child("Latitude").setValue(l.getLatitude());
        positionRef.child("Longitude").setValue(l.getLongitude());
        positionRef.child("Velocity").setValue(v);
        positionRef.child("TimeStamp").setValue(getTime());
    }

    public void updateShareList(ArrayList<String> list){
        final Firebase shareRef = getUserShareListPath();
        shareList = list;
        shareRef.setValue(list);
    }

    public void addSharingUser(final String user){
        final ArrayList<String> list = new ArrayList<>();
        final Firebase sharingRef = getUserSharingPath();
        sharingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChildren()) {
                    for (int i = 0; i < dataSnapshot.getChildrenCount(); i++) {
                        String name = dataSnapshot.child(i + "").getValue().toString();
                        list.add(name);
                    }
                }
                list.add(user);
                sharingRef.setValue(list);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    public void updateSharing(final ArrayList<String> list){
        final Firebase sharingRef = getUserSharingPath();
        sharingRef.setValue(list);
    }

    public void stopSharing(){
        final Firebase sharingRef = getUserSharingPath();
        sharingRef.removeValue();
    }

    public Firebase getUserPath(){
        return myFireBaseRef.child("User");
    }

    public Firebase getUserShareListPath(){
        return myFireBaseRef.child("User").child(username).child("ShareList");
    }

    public Firebase getUserPositionPath(String user){
        return myFireBaseRef.child("User").child(user).child("Position");
    }

    public Firebase getUserSharingPath(){
        return myFireBaseRef.child("User").child(username).child("Sharing");
    }

    private String getTime(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }
}
