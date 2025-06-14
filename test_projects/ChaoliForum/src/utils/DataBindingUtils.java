package com.daquexian.chaoli.forum.utils;

import android.databinding.Observable;
import android.support.v4.util.ArrayMap;

import com.daquexian.chaoli.forum.view.BaseActivity;

/**
 * Manage callbacks, nothing complex
 * Created by daquexian on 17-1-25.
 */

public class DataBindingUtils {
    private static ArrayMap<BaseActivity, ArrayMap<Observable, Observable.OnPropertyChangedCallback>> mGlobalMap = new ArrayMap<>();

    public static void addCallback(BaseActivity activity, Observable observable, Observable.OnPropertyChangedCallback callback) {
        ArrayMap<Observable, Observable.OnPropertyChangedCallback> localMap = mGlobalMap.get(activity);
        if (localMap == null) {
            localMap = new ArrayMap<>();
            mGlobalMap.put(activity, localMap);
        }
        observable.addOnPropertyChangedCallback(callback);
        localMap.put(observable, callback);
    }

    public static void removeCallbacks(BaseActivity activity) {
        ArrayMap<Observable, Observable.OnPropertyChangedCallback> localMap = mGlobalMap.get(activity);
        if (localMap != null) {
            for (Observable observable : localMap.keySet()) {
                observable.removeOnPropertyChangedCallback(localMap.get(observable));
            }
        }
    }
}
