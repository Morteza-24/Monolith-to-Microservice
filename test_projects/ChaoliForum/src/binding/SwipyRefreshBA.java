package com.daquexian.chaoli.forum.binding;

import android.databinding.BindingAdapter;
import android.util.Log;

import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

/**
 * SwipyRefreshLayout的BindingAdapter
 * Created by daquexian on 16-9-19.
 */
public class SwipyRefreshBA {
    private static final String TAG = "SWBA";
    //trigger the circle to animate
    @BindingAdapter("app:isRefreshing")
    public static void setRefreshing(final SwipyRefreshLayout swipyRefreshLayout, final Boolean isRefreshing) {
        swipyRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                swipyRefreshLayout.setRefreshing(isRefreshing);
            }
        });
    }

    /**
     * 为了应对奇怪的事情：把方向设为BOTTOM就没法在setRefreshing(true)之后看到小圆圈
     * @param swipyRefreshLayout ..
     * @param direction ..
     */
    @BindingAdapter("app:direction")
    public static void setDirection(final SwipyRefreshLayout swipyRefreshLayout, final SwipyRefreshLayoutDirection direction) {
        Log.d(TAG, "setDirection() called with: swipyRefreshLayout = [" + swipyRefreshLayout + "], direction = [" + direction + "]");
        swipyRefreshLayout.setDirection(direction);
    }

    @BindingAdapter("app:canRefresh")
    public static void canRefresh(final SwipyRefreshLayout swipyRefreshLayout, final Boolean canRefresh) {
        swipyRefreshLayout.setEnabled(canRefresh);
    }
}
