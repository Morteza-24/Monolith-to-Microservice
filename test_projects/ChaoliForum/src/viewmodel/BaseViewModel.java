package com.daquexian.chaoli.forum.viewmodel;

import android.content.SharedPreferences;

import com.daquexian.chaoli.forum.ChaoliApplication;

/**
 * Created by jianhao on 16-9-20.
 */

public abstract class BaseViewModel {
    protected String getString(int resId) {
        return ChaoliApplication.getAppContext().getString(resId);
    }

    protected SharedPreferences getSharedPreferences(String name, int mode) {
        return ChaoliApplication.getAppContext().getSharedPreferences(name, mode);
    }
}
