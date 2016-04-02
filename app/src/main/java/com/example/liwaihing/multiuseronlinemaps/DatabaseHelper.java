package com.example.liwaihing.multiuseronlinemaps;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.util.Base64;
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
    private String displayName, googleID, profilePic;
    private Bitmap profilePicture;

    protected DatabaseHelper(Context context){
        settings = context.getSharedPreferences("user_auth", Context.MODE_PRIVATE);
        displayName = settings.getString("name", "");
        googleID = settings.getString("googleID", "");
        profilePic = settings.getString("profilePic", "");
        updateUserProfile();
    }

    public String getGoogleID(){
        return googleID;
    }

    public String getDisplayName(){
        return displayName;
    }

    public Bitmap getProfilePicture() {
        if (profilePicture != null) {
            return profilePicture;
        }else{
            try {
                byte[] encodeByte = Base64.decode(profilePic, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
                profilePicture = bitmap;
                return bitmap;
            } catch (Exception e) {
                e.getMessage();
                return null;
            }
        }
    }

    private void updateUserProfile(){
        Firebase profileRef = getUserProfilePath(googleID);
        profileRef.child("Name").setValue(displayName);
        profileRef.child("ID").setValue(googleID);
        profileRef.child("Picture").setValue(profilePic);
    }

    public void updatePosition(Location l, double v, String a){
        Firebase positionRef = getUserPositionPath(googleID);
        positionRef.child("User").setValue(googleID);
        positionRef.child("Latitude").setValue(l.getLatitude());
        positionRef.child("Longitude").setValue(l.getLongitude());
        positionRef.child("Velocity").setValue(v);
        positionRef.child("TimeStamp").setValue(getTime());
        positionRef.child("Activity").setValue(a);
    }

    public void updateShareList(ArrayList<String> list){
        final Firebase shareRef = getUserShareListPath();
        shareRef.setValue(list);
    }

    public void addShareList(final String user){
        final ArrayList<String> list = new ArrayList<>();
        final Firebase shareRef = getUserShareListPath();
        shareRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean userExist = false;
                if (dataSnapshot.hasChildren()) {
                    for (int i = 0; i < dataSnapshot.getChildrenCount(); i++) {
                        String name = dataSnapshot.child(i + "").getValue(String.class);
                        list.add(name);
                        if (name.equals(user)) {
                            userExist = true;
                        }
                    }
                }
                if (!userExist) {
                    list.add(user);
                    shareRef.setValue(list);
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    public void addSharingUser(final String user, final String path){
        final ArrayList<String> list = new ArrayList<>();
        final Firebase sharingRef = getUserSharingPath(path);
        sharingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChildren()) {
                    for (int i = 0; i < dataSnapshot.getChildrenCount(); i++) {
                        String name = dataSnapshot.child(i + "").getValue(String.class);
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

    public void removeSharingUser(final String user, final String path){
        final ArrayList<String> list = new ArrayList<>();
        final Firebase sharingRef = getUserSharingPath(path);
        sharingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChildren()) {
                    for (int i = 0; i < dataSnapshot.getChildrenCount(); i++) {
                        String name = dataSnapshot.child(i + "").getValue(String.class);
                        list.add(name);
                    }
                }
                list.remove(user);
                sharingRef.setValue(list);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    public void stopSharing(){
        final Firebase sharingRef = getUserSharingPath(googleID);
        sharingRef.removeValue();
        for(String name : CommonUserList.getUserSharingList()){
            removeSharingUser(googleID, name);
        }
    }

    public void inviteUserSharing(String user){
        Firebase inviteRef = getUserInvitationPath(user);
        inviteRef.child(googleID).setValue("Pending");

        Firebase requestRef = getUserRequestPath(googleID);
        requestRef.child(user).setValue("Pending");
    }

    public void removeInvitation(String user){
        Firebase inviteRef = getUserInvitationPath(googleID);
        inviteRef.child(user).removeValue();

        Firebase requestRef = getUserRequestPath(user);
        requestRef.child(googleID).removeValue();
    }

    public void removeAllRequest(){
        Firebase requestRef = getUserRequestPath(googleID);
        requestRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChildren()) {
                    for (int i = 0; i < dataSnapshot.getChildrenCount(); i++) {
                        String name = dataSnapshot.child(i + "").getValue(String.class);
                        if(name!=null){
                            Firebase inviteRef = getUserInvitationPath(name);
                            inviteRef.child(googleID).removeValue();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    public Firebase getUserPath(){
        return myFireBaseRef.child("User");
    }

    public Firebase getUserProfilePath(String user){
        return myFireBaseRef.child("User").child(user).child("Profile");
    }

    public Firebase getUserShareListPath(){
        return myFireBaseRef.child("User").child(googleID).child("ShareList");
    }

    public Firebase getUserPositionPath(String user){
        return myFireBaseRef.child("User").child(user).child("Position");
    }

    public Firebase getUserSharingPath(String user){
        return myFireBaseRef.child("User").child(user).child("Sharing");
    }

    public Firebase getUserInvitationPath(String user){
        return myFireBaseRef.child("User").child(user).child("Invitation");
    }

    public Firebase getUserRequestPath(String user){
        return myFireBaseRef.child("User").child(user).child("Request");
    }

    private String getTime(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }
}
