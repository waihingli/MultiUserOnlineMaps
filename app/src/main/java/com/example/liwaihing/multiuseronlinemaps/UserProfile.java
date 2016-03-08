package com.example.liwaihing.multiuseronlinemaps;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.firebase.client.Firebase;

/**
 * Created by WaiHing on 8/3/2016.
 */
public class UserProfile {
    private String displayName;
    private String googleID;
    private String profilePic;
    private boolean isSharing = false;

    protected UserProfile(String id){
        googleID = id;
    }

    public String getGoogleID() {
        return googleID;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String s) {
        displayName = s;
    }

    public void setProfilePic(String s) {
        profilePic = s;
    }

    public Bitmap getProfilePic(){
        try{
            byte[] encodeByte=Base64.decode(profilePic,Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            return bitmap;
        }catch(Exception e){
            e.getMessage();
            return null;
        }
    }

    public void setIsSharing(boolean b){
        isSharing = b;
    }

    public boolean getIsSharing(){
        return isSharing;
    }

    public UserProfile getUserProfile(String name){
        if (name.equals(googleID)){
            return this;
        }
        return null;
    }

}
