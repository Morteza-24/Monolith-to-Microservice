package com.daquexian.chaoli.forum.viewmodel;

import android.content.SharedPreferences;
import android.databinding.Observable;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.util.Log;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.meta.Channel;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.utils.ConversationUtils;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by jianhao on 16-9-21.
 */

public class PostActionVM extends BaseViewModel {
    private static final String TAG = "PostActionVM";
    public ObservableField<String> title = new ObservableField<>();
    public ObservableField<String> content = new ObservableField<>();
    private String prevContent;
    private String prevTitle;
    public ObservableInt channelId = new ObservableInt();

    public ObservableBoolean postComplete = new ObservableBoolean(false);
    public ObservableInt showToast = new ObservableInt();
    public ObservableField<String> toastContent = new ObservableField<>();
    public ObservableBoolean showWelcome = new ObservableBoolean();
    public ObservableBoolean showDialog = new ObservableBoolean();

    public ObservableBoolean updateContentRichText = new ObservableBoolean();
    public ObservableBoolean updateTitleRichText = new ObservableBoolean();
    public ObservableBoolean demoMode = new ObservableBoolean();

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    //private SharedPreferences globalSP;
    //private SharedPreferences.Editor globalSPEditor;
    private Channel preChannel = Channel.caff;
    private Channel curChannel;

    private static final String DRAFT_CONTENT = "draft_content";
    private static final String DRAFT_TITLE = "draft_title";
    private static final String DRAFT_CHANNEL = "draft_channel";

    public PostActionVM() {
        sharedPreferences = ChaoliApplication.getAppContext().getSharedPreferences(TAG, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        title.set(sharedPreferences.getString(DRAFT_TITLE, ""));
        content.set(sharedPreferences.getString(DRAFT_CONTENT, ""));
        prevContent = content.get();
        channelId.set(sharedPreferences.getInt(DRAFT_CHANNEL, Channel.caff.getChannelId()));
        curChannel = Channel.getChannel(channelId.get());

        content.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {

            }
        });
    }

    public void postConversation() {
        if (content.get().length() == 0) {
            toastContent.set(getString(R.string.content_cannot_be_null));
            showToast.notifyChange();
            return;
        }

        showDialog.set(true);
        ConversationUtils.postConversation(title.get(), content.get(), new ConversationUtils.PostConversationObserver() {
            @Override
            public void onPostConversationSuccess(int conversationId) {
                Log.d(TAG, "onPostConversationSuccess() called with: conversationId = [" + conversationId + "]");
                editor.clear().apply();
                postComplete.notifyChange();
                //postComplete.set(true);
                showDialog.set(false);
            }

            @Override
            public void onPostConversationFailure(int statusCode) {
                Log.d(TAG, "onPostConversationFailure() called with: statusCode = [" + statusCode + "]");
                toastContent.set(getString(R.string.network_err));
                showToast.notifyChange();
                showDialog.set(false);
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

    // TODO: 16-11-16 这样写很不利于维护，理想的做法应该是把自动显示表情的功能集中到自定义控件里，外部“即插即用”，但偏偏和双向绑定冲突（引起不断循环），最好考虑一种解决办法
    public void doAfterContentChanged() {
        String newContent = content.get();
        if (newContent.equals(prevContent)) return;
        prevContent = newContent;
        //updateContentRichText.notifyChange();
        saveContent(newContent);
    }

    public void doAfterTitleChanged() {
        String newTitle = title.get();
        if (newTitle.equals(prevTitle)) return;
        prevTitle = newTitle;
        //updateTitleRichText.notifyChange();
        saveTitle(newTitle);
    }

    public void setChannelId(final int channelId) {
        this.channelId.set(channelId);
        preChannel = curChannel;
        saveChannelId(channelId);
        ConversationUtils.setChannel(channelId, new ConversationUtils.SetChannelObserver() {
            @Override
            public void onSetChannelSuccess() {
                // what need to do has done in advance
            }

            @Override
            public void onSetChannelFailure(int statusCode) {   // restore
                showToast.notifyChange();
                toastContent.set(getString(R.string.network_err));

                PostActionVM.this.channelId.set(preChannel.getChannelId());
                curChannel = preChannel;
                saveChannelId(curChannel.getChannelId());
            }
        });
    }

    public void saveTitle(String title) {
        editor.putString(DRAFT_TITLE, title).apply();
    }

    private void saveContent(String content) {
        editor.putString(DRAFT_CONTENT, content).apply();
    }

    private void saveChannelId(int channelId) {
        editor.putInt(DRAFT_CHANNEL, channelId).apply();
    }
}
