package com.daquexian.chaoli.forum.meta;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.util.AttributeSet;
import android.widget.CheckBox;

import com.daquexian.chaoli.forum.model.Post;

import java.util.List;

/**
 * 和OnlineImgTextView类似
 * 用于答题时显示选项中的LaTeX
 * Created by jianhao on 16-9-4.
 */
public class OnlineImgCheckBox extends CheckBox implements IOnlineImgView {
    private OnlineImgImpl mImpl;

    public OnlineImgCheckBox(Context context, @Nullable List<Post.Attachment> attachmentList)
    {
        super(context);
        init(context, attachmentList);
    }

    public OnlineImgCheckBox(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context, null);
    }

    public OnlineImgCheckBox(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context, null);
    }

    public void setText(String text) {
        mImpl.setText(text);
    }

    public void setText(String text, OnlineImgImpl.OnCompleteListener listener) {
        mImpl.setListener(listener);
        mImpl.setText(text);
    }

    private void init(Context context, @Nullable List<Post.Attachment> attachmentList) {
        mImpl = new OnlineImgImpl(this);
        mImpl.mAttachmentList = attachmentList;
    }

    @Override
    public void setText(SpannableStringBuilder builder) {
        ((CheckBox) this).setText(builder);
    }
}
