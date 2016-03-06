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
    public static DatabaseHelper instance = null;
    public static final String PATH = "https://multiuseronlinemap.firebaseio.com/";
    private Firebase myFireBaseRef = new Firebase(PATH);
    private SharedPreferences settings;
    private String username;
    ArrayList<String> sharelist = new ArrayList<>();
    boolean userExist, haveShareList;

    static public DatabaseHelper getInstance(Context context){
        if(instance == null){
            instance = new DatabaseHelper(context);
        }
        return instance;
    }

    protected DatabaseHelper(Context context){
        settings = context.getSharedPreferences("user_auth", Context.MODE_PRIVATE);
        username = settings.getString("username", "");
        dbOnload();
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

    public void dbOnload(){
        getDbShareList();
    }

    public void updatePosition(Location l, double v){
        Firebase positionRef = myFireBaseRef.child("User").child(username).child("Position");
        positionRef.child("Latitude").setValue(l.getLatitude());
        positionRef.child("Longitude").setValue(l.getLongitude());
        positionRef.child("Velocity").setValue(v);
        positionRef.child("TimeStamp").setValue(getTime());
    }

    public void getDbShareList(){
        final Firebase shareRef = myFireBaseRef.child("User").child(username).child("ShareList");
        shareRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChildren()) {
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        if(child.getValue() != null){
                            String name = child.getValue().toString();
                            sharelist.add(name);
                        }
                        if(sharelist.size() == dataSnapshot.getChildrenCount()){
                            break;
                        }
                    }
                } else{
                    haveShareList = false;
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    public ArrayList<String> getShareList(){
        return sharelist;
    }

    public boolean haveShareList(){
        return haveShareList;
    }

    public boolean checkUserExist(final String name){
        userExist = false;
        final Firebase usetRef = myFireBaseRef.child("User");
        usetRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild(name)){
                   userExist = true;
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
        return userExist;
    }

    public void updateShareList(ArrayList<String> list){
        final Firebase shareRef = myFireBaseRef.child("User").child(username).child("ShareList");
        sharelist = list;
        shareRef.setValue(list);
    }

    private String getTime(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }
}
