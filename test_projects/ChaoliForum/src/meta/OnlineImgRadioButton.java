package com.daquexian.chaoli.forum.meta;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.util.AttributeSet;
import android.widget.RadioButton;

import com.daquexian.chaoli.forum.model.Post;

import java.util.List;

/**
 * 和OnlineImgTextView类似，只是继承了RadioButton
 * 用于答题时显示选项中的LaTeX
 * Created by jianhao on 16-9-4.
 */
public class OnlineImgRadioButton extends RadioButton implements IOnlineImgView {
    OnlineImgImpl mImpl;

    public OnlineImgRadioButton(Context context, @Nullable List<Post.Attachment> attachmentList)
    {
        super(context);
        init(context, attachmentList);
    }

    public OnlineImgRadioButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context, null);
    }

    public OnlineImgRadioButton(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context, null);
    }

    public void setText(String text) {
        mImpl.setText(text);
    }

    public void setText(String text, OnlineImgImpl.OnCompleteListener listener) {
        mImpl.setText(text);
        mImpl.setListener(listener);
    }

    @Override
    public void setText(SpannableStringBuilder builder) {
        ((RadioButton) this).setText(builder);
    }

    private void init(Context context, @Nullable List<Post.Attachment> attachmentList) {
        mImpl = new OnlineImgImpl(this);
        mImpl.mAttachmentList = attachmentList;
    }
}
