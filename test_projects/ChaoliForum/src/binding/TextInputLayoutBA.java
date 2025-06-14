package com.daquexian.chaoli.forum.binding;

import android.databinding.BindingAdapter;
import android.support.design.widget.TextInputLayout;

/**
 * Created by jianhao on 16-10-6.
 */

public class TextInputLayoutBA {
    @BindingAdapter("app:error")
    public static void setError(TextInputLayout textInputLayout, String error) {
        textInputLayout.setError(error);
    }
}
