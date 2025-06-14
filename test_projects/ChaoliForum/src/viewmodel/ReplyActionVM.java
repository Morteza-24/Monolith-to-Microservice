package com.daquexian.chaoli.forum.viewmodel;

import android.content.SharedPreferences;
import android.databinding.BindingAdapter;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.util.Log;
import android.view.View;

import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.utils.PostUtils;

import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by jianhao on 16-9-21.
 */

public class ReplyActionVM extends BaseViewModel {
    private static final String TAG = "ReplyActionVM";

    public ObservableInt flag = new ObservableInt();
    public ObservableInt conversationId = new ObservableInt();
    public ObservableInt postId = new ObservableInt();
    public ObservableField<String> replyTo = new ObservableField<>();
    public ObservableField<String> replyMsg = new ObservableField<>();
    public ObservableField<String> content = new ObservableField<>("");
    private String prevContent;
    public ObservableBoolean selectionLast = new ObservableBoolean();

    public ObservableBoolean showDialog = new ObservableBoolean();
    public ObservableBoolean showWelcome = new ObservableBoolean();
    public ObservableInt showToast = new ObservableInt();
    public ObservableField<String> toastContent = new ObservableField<>();
    public ObservableBoolean replyComplete = new ObservableBoolean(false);
    public ObservableBoolean editComplete = new ObservableBoolean(false);
    public ObservableBoolean demoMode = new ObservableBoolean();
    public ObservableBoolean updateRichText = new ObservableBoolean();

    private SharedPreferences sp;
    private SharedPreferences.Editor e;

    public ReplyActionVM(int flag, int conversationId, int postId, String replyTo, String replyMsg) {
        this.flag.set(flag);
        this.conversationId.set(conversationId);
        this.postId.set(postId);
        this.replyTo.set(replyTo);
        this.replyMsg.set(replyMsg);
        sp = getSharedPreferences(TAG, MODE_PRIVATE);
        e = sp.edit();
        String draft = sp.getString(String.valueOf(conversationId), "");
        if (!"".equals(draft)) content.set(draft);
        if (this.postId.get() != -1) content.set(String.format(Locale.ENGLISH, "[quote=%d:@%s]%s[/quote]\n", this.postId.get(), this.replyTo.get(), this.replyMsg.get()));
        //selectionLast.notifyChange();
        //selectionLast.set(content.get().length());
    }

    public void reply() {
        showDialog.set(true);
        PostUtils.reply(conversationId.get(), content.get(), new PostUtils.ReplyObserver()
        {
            @Override
            public void onReplySuccess()
            {
                toastContent.set(getString(R.string.reply_successfully));
                showToast.notifyChange();
                clearSaved();
                showDialog.set(false);
                replyComplete.set(true);
            }

            @Override
            public void onReplyFailure(int statusCode)
            {
                showDialog.set(false);
                toastContent.set("Fail: " + statusCode);
                showToast.notifyChange();
            }
        });

    }

    public void edit() {
        PostUtils.edit(postId.get(), content.get(), new PostUtils.EditObserver()
        {
            @Override
            public void onEditSuccess()
            {
                showToast.set(showToast.get() + 1);
                toastContent.set("Post"); // TODO: 16-10-11
                editComplete.set(true);
            }

            @Override
            public void onEditFailure(int statusCode)
            {
                showToast.set(showToast.get() + 1);
                toastContent.set("Fail:" + statusCode); // TODO: 16-10-11
            }
        });
    }

    public void changeDemoMode() {
        demoMode.set(!demoMode.get());
        SharedPreferences globalSP = getSharedPreferences(Constants.GLOBAL, MODE_PRIVATE);
        if (globalSP.getBoolean(Constants.FIRST_ENTER_DEMO_MODE, true) && demoMode.get()) {
            showWelcome.notifyChange();
            globalSP.edit().putBoolean(Constants.FIRST_ENTER_DEMO_MODE, false).apply();
        }
    }

    public void doAfterContentChanged() {
        Log.d(TAG, "doAfterContentChanged() called");
        String newContent = content.get();
        if (newContent.equals(prevContent)) return;
        prevContent = newContent;
        updateRichText.notifyChange();
        saveReply();
    }

    private void saveReply() {
        e.putString(String.valueOf(conversationId.get()), content.get()).apply();
    }

    private void clearSaved() {
        e.clear().apply();
    }

    @BindingAdapter("alpha")
    public static void setAlpha(View view,float alpha){
        view.setAlpha(alpha);
    }
}

