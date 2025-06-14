package com.daquexian.chaoli.forum.binding;

/**
 * Created by jianhao on 16-9-27.
 */

public class DefaultLayoutSelector<T> extends LayoutSelector<T>{
    int mLayoutId;

    public DefaultLayoutSelector(int layoutId) {
        mLayoutId = layoutId;
    }

    @Override
    int getLayout(int type) {
        return mLayoutId;
    }

    @Override
    int getType(T item) {
        return 0;
    }
}
