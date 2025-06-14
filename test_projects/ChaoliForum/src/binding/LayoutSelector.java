package com.daquexian.chaoli.forum.binding;

/**
 * Created by jianhao on 16-9-27.
 */

public abstract class LayoutSelector<T> {
    abstract int getLayout(int type);
    abstract int getType(T item);
    public static final int FOOTER_VIEW = -1;
}
