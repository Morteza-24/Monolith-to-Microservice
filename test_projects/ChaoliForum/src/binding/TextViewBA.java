package com.daquexian.chaoli.forum.binding;

import android.databinding.BindingAdapter;
import android.text.TextPaint;
import android.widget.TextView;

/**
 * Binding adapter for TextView
 * Created by jianhao on 16-10-3.
 */

public class TextViewBA {
    @SuppressWarnings("unused")
    private static final String TAG = "TVBA";
    @BindingAdapter("app:bold")
    public static void setBold(TextView textView, Boolean isBold) {
        TextPaint tp = textView.getPaint();
        tp.setFakeBoldText(isBold);
    }
}

