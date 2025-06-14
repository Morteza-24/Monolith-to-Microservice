package com.daquexian.chaoli.forum.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.model.User;
import com.google.gson.Gson;

/**
 * Created by jianhao on 16-9-3.
 * Manage and store data about user himself.
 */
public class Me {
    private static User me = new User();

    public static void clear(){
        me = new User();
    }

    public static boolean isEmpty(){
        return me.isEmpty();
    }

    public static int getMyUserId(){
        return me.getUserId();
    }

    public static String getMyUsername(){
        return me.getUsername();
    }

    public static String getMyAvatarSuffix(){
        return me.getAvatarSuffix();
    }

    @SuppressWarnings("unused")
    public static String getMyAvatarURL(){
        return Constants.avatarURL + "avatar_" + getMyUserId() + "." + getMyAvatarSuffix();
    }

    public static String getMyStatus(){
        return me.getStatus();
    }

    public static String getMySignature(){
        return me.getPreferences() != null && me.getPreferences().getSignature() != null ? me.getPreferences().getSignature() : "";
    }

    public static Boolean getMyPrivateAdd(){
        return me.getPreferences().getPrivateAdd();
    }

    public static Boolean getMyStarOnReply(){
        return me.getPreferences().getStarOnReply();
    }

    public static Boolean getMyStarPrivate(){
        return me.getPreferences().getStarPrivate();
    }

    public static Boolean getMyHideOnline(){
        return me.getPreferences().getHideOnline() != null ? me.getPreferences().getHideOnline() : false;
    }

    public static User.Preferences getPreferences() {
        return me.getPreferences();
    }

    public static String getUsername(){
        return me.getUsername();
    }

    public static void setUsername(String username){
        me.setUsername(username);
    }

    @SuppressWarnings("unused")
    public static void setPreferences(User.Preferences preferences){
        me.setPreferences(preferences);
    }


    public static int getUserId() {
        return getMyUserId();
    }

    public static void setUserId(int userId) {
        me.setUserId(userId);
    }

    public static String getStatus() {
        return me.getStatus();
    }

    public static void setStatus(String status) {
        me.setStatus(status);
    }

    public static String getAvatarSuffix() {
        return me.getAvatarSuffix();
    }

    public static void setAvatarSuffix(String avatarSuffix) {
        me.setAvatarSuffix(avatarSuffix);
    }

    public static void setProfile(Context context, User user) {
        user.setUsername(me.getUsername());
        user.setUserId(me.getUserId());
        me = user;
        me.setEmpty(false);
        if(user.getAvatarSuffix() == null){
            user.setAvatarSuffix(Constants.NONE);
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getUsername(), new Gson().toJson(user));
        editor.apply();
    }

    private static void setInstanceFromJSONStr(Context context, String jsonStr){
        User user2 = new Gson().fromJson(jsonStr, User.class);
        user2.setUserId(me.getUserId());
        user2.setUsername(me.getUsername());
        me = user2;
        me.setEmpty(false);
        if(getAvatarSuffix() == null){
            me.setAvatarSuffix(Constants.NONE);
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getMyUsername(), new Gson().toJson(me));
        editor.apply();
    }
    public static void setInstanceFromSharedPreference(Context context, String username) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        if(sharedPreferences.contains(username)) {
            String info = sharedPreferences.getString(username, "bing mei you");
            setInstanceFromJSONStr(context, info);
        }
    }

}
