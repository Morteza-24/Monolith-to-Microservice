package com.daquexian.chaoli.forum.meta;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.util.AttributeSet;
import android.widget.TextView;

import com.daquexian.chaoli.forum.model.Post;

import java.util.List;

/**
 * 需要在线获取的图片交给它来显示
 * 其他处理在SFXParser3中进行
 */
public class OnlineImgTextView extends TextView implements IOnlineImgView
{
	private OnlineImgImpl mImpl;

	private static final String TAG = "OnlineImgTextView";

	public OnlineImgTextView(Context context, @Nullable List<Post.Attachment> attachmentList)
	{
		super(context);
		init(context, attachmentList);
	}

	public OnlineImgTextView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(context, null);
	}

	public OnlineImgTextView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		init(context, null);
	}

	public void setText(String text){
		mImpl.setText(text);
	}

	public void setText(String text, OnlineImgImpl.OnCompleteListener listener) {
		mImpl.setListener(listener);
		mImpl.setText(text);
	}

	private void init(Context context, @Nullable List<Post.Attachment> attachmentList) {
		setTextIsSelectable(true);
		mImpl = new OnlineImgImpl(this);
		mImpl.mAttachmentList = attachmentList;
	}

	@Override
	public void setText(SpannableStringBuilder builder) {
		((TextView) this).setText(builder);
	}
}
