package com.daquexian.chaoli.forum.meta;

import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;

/**
 * Extended AppBarLayout, add getOffset and getState method and so on.
 * Created by daquexian on 16-12-2.
 */

public class MyAppBarLayout extends AppBarLayout
        implements AppBarLayout.OnOffsetChangedListener {

    private State state;
    private int offset;
    private OnStateChangeListener onStateChangeListener;

    public MyAppBarLayout(Context context) {
        super(context);
    }

    public MyAppBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!(getLayoutParams() instanceof CoordinatorLayout.LayoutParams)
                || !(getParent() instanceof CoordinatorLayout)) {
            throw new IllegalStateException(
                    "MyAppBarLayout must be a direct child of CoordinatorLayout.");
        }
        addOnOffsetChangedListener(this);
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        if (verticalOffset == 0) {
            if (onStateChangeListener != null && state != State.EXPANDED) {
                onStateChangeListener.onStateChange(State.EXPANDED);
            }
            state = State.EXPANDED;
        } else if (Math.abs(verticalOffset) >= appBarLayout.getTotalScrollRange()) {
            if (onStateChangeListener != null && state != State.COLLAPSED) {
                onStateChangeListener.onStateChange(State.COLLAPSED);
            }
            state = State.COLLAPSED;
        } else {
            if (onStateChangeListener != null && state != State.IDLE) {
                onStateChangeListener.onStateChange(State.IDLE);
            }
            state = State.IDLE;
        }
        offset = verticalOffset;
    }

    public int getOffset() {
        return offset;
    }

    public void setOnStateChangeListener(OnStateChangeListener listener) {
        this.onStateChangeListener = listener;
    }

    public interface OnStateChangeListener {
        void onStateChange(State toolbarChange);
    }

    public State getState() {
        return state;
    }

    public enum State {
        COLLAPSED,
        EXPANDED,
        IDLE
    }
}
