package com.example.liwaihing.multiuseronlinemaps;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;

import com.firebase.client.AuthData;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.text.SimpleDateFormat;
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

    protected DatabaseHelper(Context context){
        settings = context.getSharedPreferences("user_auth", Context.MODE_PRIVATE);
        username = settings.getString("username", "");
    }

    public void authentication(){
        myFireBaseRef.authWithOAuthToken("google", "<OAuth Token>", new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                // the Google user is now authenticated with your Firebase app
            }
            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                // there was an error
            }
        });
//
//        SimpleLogin authClient = new SimpleLogin(myRef, getApplicationContex());
//        authClient.checkAuthStatus(new SimpleLoginAuthenticatedHandler() {
//            @Override
//            public void authenticated(FirebaseSimpleLoginError error, FirebaseSimpleLoginUser user) {
//                if (error != null) {
//                    // Oh no! There was an error performing the check
//                } else if (user == null) {
//                    // No user is logged in
//                } else {
//                    // There is a logged in user
//                }
//            }
//        });
    }

    public void updateDb(Location l, double v){
        Firebase positionRef = myFireBaseRef.child("User").child(username).child("Position");
        positionRef.child("Latitude").setValue(l.getLatitude());
        positionRef.child("Longitude").setValue(l.getLongitude());
        positionRef.child("Velocity").setValue(v);
        positionRef.child("TimeStamp").setValue(getTime());
    }

    public void onDbDataChange(){
        String childName = "position_" + username ;
        Firebase userRef = myFireBaseRef.child(childName);
        myFireBaseRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

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
        });
    }

    public String getTime(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }
}
