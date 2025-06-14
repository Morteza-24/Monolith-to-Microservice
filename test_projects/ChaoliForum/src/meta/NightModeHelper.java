package com.daquexian.chaoli.forum.meta;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatDelegate;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.viewmodel.BaseViewModel;

import static android.content.Context.MODE_PRIVATE;

/**
 * The helper to night mode
 * Created by root on 1/22/17.
 */

public class NightModeHelper{
    private static final String MODE = "night_mode";
    private static final String NIGHT = "night";
    private static final String DAY = "day";

    private static BaseViewModel mViewModel;

    private static SharedPreferences getSp(){
        return ChaoliApplication.getSp();
    }

    public static boolean isDay(){
        String temp = getSp().getString(MODE,DAY);
        return temp.equals(DAY);
    }

    public static void changeMode(BaseViewModel viewModel) {
        if (isDay()) {
            setNight();
        } else {
            setDay();
        }
        mViewModel = viewModel;
    }

    public static BaseViewModel getViewModel() {
        return mViewModel;
    }

    public static void removeViewModel() {
            mViewModel = null;
    }

    private static void setNight(){
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        SharedPreferences.Editor editor = getSp().edit();
        editor.putString(MODE,NIGHT);
        editor.apply();
    }

    private static void setDay(){
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        SharedPreferences.Editor editor = getSp().edit();
        editor.putString(MODE,DAY);
        editor.apply();
    }
}
