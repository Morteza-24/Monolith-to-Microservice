package com.daquexian.chaoli.forum.meta;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.model.Post;
import com.daquexian.chaoli.forum.utils.MyUtils;

import java.util.List;

/**
 * 用于显示帖子中的引用
 * Created by jianhao on 16-8-26.
 */
public class QuoteView extends LinearLayout {
    OnlineImgTextView mTextView;
    Button mButton;
    Boolean mCollapsed;
    Context mContext;

    private static final String TAG = "QuoteView";
    private static int BG_COLOR ;

    public QuoteView(Context context, List<Post.Attachment> attachmentList) {
        super(context);
        init(context, attachmentList);
    }
    public QuoteView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context, null);
    }
    public QuoteView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
        init(context, null);
    }

    /*public void setPost(final Post post) {
        mPostContentView.setPost(post);
        mTextView.setText(post.getContent().replaceAll("\\[quote(.*?)\\[/quote]", "..."));
        mTextView.post(new Runnable() {
            @Override
            public void run() {
                if (mTextView.getLineCount() > 3) {
                    collapse();
                    //mButton.setTextSize(BUTTON_TEXT_SIZE);
                    //LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                    mButton.setTextSize(10);
                    //mButton.setLayoutParams(params);
                    //mButton.setMinHeight(0);
                    mButton.setBackgroundColor(BG_COLOR);
                    mButton.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Log.d(TAG, "onClick() called with: " + "view = [" + view + "]");
                            if (mCollapsed) {
                                expand();
                            } else {
                                collapse();
                            }
                        }
                    });
                    addView(mButton);
                }
            }
        });
    }*/
    public void setText(String content) {
        mTextView.setText(MyUtils.formatQuote(content));
        mTextView.post(new Runnable() {
            @Override
            public void run() {
                if (mTextView.getLineCount() > 3) {
                    collapse();
                    //mButton.setTextSize(BUTTON_TEXT_SIZE);
                    //LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                    mButton.setTextSize(10);
                    //mButton.setLayoutParams(params);
                    //mButton.setMinHeight(0);
                    mButton.setBackgroundColor(BG_COLOR);
                    mButton.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Log.d(TAG, "onClick() called with: " + "view = [" + view + "]");
                            if (mCollapsed) {
                                expand();
                            } else {
                                collapse();
                            }
                        }
                    });
                    addView(mButton);
                }
            }
        });
    }

    private void collapse(){
        Log.d(TAG, "collapse() called with: " + "");
        mTextView.setMaxLines(3);
        mTextView.setEllipsize(TextUtils.TruncateAt.END);
        //mTextView.setVisibility(VISIBLE);
        //mPostContentView.setVisibility(GONE);
        mButton.setText(R.string.expand);
        mCollapsed = true;
    }

    private void expand(){
        mTextView.setMaxLines(Integer.MAX_VALUE);
        mTextView.setEllipsize(null);
        //mTextView.setVisibility(GONE);
        //mPostContentView.setVisibility(VISIBLE);
        mButton.setText(R.string.collapse);
        mCollapsed = false;
    }

    private void init(Context context, List<Post.Attachment> attachmentList) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.QuoteViewBackground,value,true);
        BG_COLOR = value.data;
        mContext = context;
        mCollapsed = false;
        mTextView = new OnlineImgTextView(context, attachmentList);
        //mButton = new Button(context);
        mButton = (Button) LinearLayout.inflate(context, R.layout.button, null);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mTextView.setLayoutParams(params);
        mTextView.setBackgroundColor(BG_COLOR);
        addView(mTextView);
        mTextView.setTextIsSelectable(true);
    }
}
