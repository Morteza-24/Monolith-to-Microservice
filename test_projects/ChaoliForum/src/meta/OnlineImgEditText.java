package com.daquexian.chaoli.forum.meta;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.util.AttributeSet;
import android.widget.EditText;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.model.Post;

import java.util.List;

/**
 * Created by daquexian on 16-11-15.
 */

public class OnlineImgEditText extends EditText implements IOnlineImgView {
    private OnlineImgImpl mImpl;
    private boolean onlineImgEnabled = false;

    public static final String TAG = "OnlineImgEditText";

    public OnlineImgEditText(Context context, @Nullable List<Post.Attachment> attachmentList)
    {
        super(context);
        init(context, attachmentList);
    }

    public OnlineImgEditText(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context, null);
    }

    public OnlineImgEditText(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context, null);
    }

    public void update() {
        mySetText(getText().toString());
    }

    public void setOnlineImgEnabled(boolean onlineImgEnabled) {
        this.onlineImgEnabled = onlineImgEnabled;
    }

    /*
        如果命名成setText(String text)就会引起Data Binding的双向绑定导致的循环。。不创建这个方法或者是其他名字就不会，好奇怪哈哈哈
         */
    public void mySetText(String text){
        if (onlineImgEnabled) mImpl.setText(text);
        else setText(SFXParser3.parse(ChaoliApplication.getAppContext(), text, null));
    }

    public void mySetText(String text, OnlineImgImpl.OnCompleteListener listener) {
        if (onlineImgEnabled) {
            mImpl.setListener(listener);
            mImpl.setText(text);
        } else {
            SpannableStringBuilder builder = SFXParser3.parse(ChaoliApplication.getAppContext(), text, null);
            super.setText(builder);
            listener.onComplete(builder);
        }
    }

    private void init(Context context, @Nullable List<Post.Attachment> attachmentList) {
        setTextIsSelectable(true);
        mImpl = new OnlineImgImpl(this);
        mImpl.mAttachmentList = attachmentList;
    }

    @Override
    public void setText(SpannableStringBuilder builder) {
        ((EditText) this).setText(builder);
    }
}
