package com.example.liwaihing.multiuseronlinemaps;

import java.util.ArrayList;

/**
 * Created by WaiHing on 8/3/2016.
 */
public class CommonUserList {
    private static ArrayList<UserProfile> userProfileList = new ArrayList<>();
    private static ArrayList<String> userSharingList = new ArrayList<>();

    public static ArrayList<UserProfile> getUserProfileList() {
        return userProfileList;
    }

    public static void addUserProfileList(UserProfile u){
        userProfileList.add(u);
    }

    public static void removeUserProfileList(UserProfile u){
        userProfileList.remove(u);
    }

    public static ArrayList<String> getUserSharingList() {
        return userSharingList;
    }

    public static void addUseSharingList(String s){
        userSharingList.add(s);
    }

    public static void removeUserSharingList(String s){
        userSharingList.remove(s);
    }

}
