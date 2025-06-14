package com.daquexian.chaoli.forum.viewmodel;

import android.databinding.ObservableArrayList;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.util.Log;
import android.view.View;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.binding.ConversationLayoutSelector;
import com.daquexian.chaoli.forum.binding.LayoutSelector;
import com.daquexian.chaoli.forum.data.Me;
import com.daquexian.chaoli.forum.meta.Channel;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.model.Conversation;
import com.daquexian.chaoli.forum.model.ConversationListResult;
import com.daquexian.chaoli.forum.model.NotificationList;
import com.daquexian.chaoli.forum.network.MyOkHttp;
import com.daquexian.chaoli.forum.network.MyRetrofit;
import com.daquexian.chaoli.forum.utils.AccountUtils;
import com.daquexian.chaoli.forum.utils.LoginUtils;
import com.daquexian.chaoli.forum.utils.MyUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * ViewModel of MainActivity
 * Created by jianhao on 16-9-19.
 */
public class MainActivityVM extends BaseViewModel {
    private final String TAG = "MainActivityVM";

    public ObservableArrayList<Conversation> conversationList = new ObservableArrayList<>();

    public ObservableBoolean canRefresh = new ObservableBoolean(true);
    public ObservableBoolean isRefreshing = new ObservableBoolean();

    public ObservableField<Integer> myUserId = new ObservableField<>(-1);
    public ObservableField<String> myUsername = new ObservableField<>();
    public ObservableField<String> myAvatarSuffix = new ObservableField<>();
    public ObservableField<String> mySignature = new ObservableField<>();

    /**
     * isLoggedIn is only used to determine whether there are available username, signature etc to show
     * To check account status, please use LoginUtils.isLoggedIn() or Me.isEmpty()
     */
    public ObservableBoolean isLoggedIn = new ObservableBoolean(false);
    public ObservableBoolean loginComplete = new ObservableBoolean(false);

    public ObservableBoolean smoothToFirst = new ObservableBoolean(false);

    public ObservableInt goToHomepage = new ObservableInt();
    public ObservableInt goToLogin = new ObservableInt();
    public ObservableInt goToConversation = new ObservableInt();
    public Conversation clickedConversation;
    public ObservableInt notificationsNum = new ObservableInt(0);
    public ObservableBoolean showLoginProcessDialog = new ObservableBoolean(false);
    public ObservableBoolean toFirstLoadConversation = new ObservableBoolean();
    public ObservableInt selectedItem = new ObservableInt(-1);
    public ObservableInt goToPost = new ObservableInt();
    public ObservableBoolean failed = new ObservableBoolean();
    public ObservableBoolean showToast = new ObservableBoolean();
    public String toastContent;

    public LayoutSelector<Conversation> layoutSelector = new ConversationLayoutSelector();

    private static int RETURN_ERROR = -1;
    private CompositeSubscription subscription;
    private String channel;
    private int page;

    private Boolean canAutoLoad = false;

    public MainActivityVM() {
        //conversationList.add(new Conversation());
        subscription = new CompositeSubscription();
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    private void getList(final int page) {
        getList(page, false);
    }
    private void getList(final int page, final Boolean refresh)
    {
        Log.d(TAG, "getList() called with: page = [" + page + "]");
        subscription.add(MyRetrofit.getService()
                .listConversations(channel, "#第 " + page + " 页")
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        showCircle();
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ConversationListResult>() {
                    @Override
                    public void onCompleted() {
                        removeCircle();
                    }

                    @Override
                    public void onError(Throwable e) {
                        removeCircle();
                        if (refresh) {
                            failed.set(true);
                            failed.notifyChange();
                        }
                    }

                    @Override
                    public void onNext(ConversationListResult conversationListResult) {
                        //conversationList.remove(conversationList.size() - 1);
                        int oldLen = conversationList.size();
                        Log.d(TAG, "onNext: " + oldLen);
                        List<Conversation> newConversationList = conversationListResult.getResults();
                        canAutoLoad = true;
                        if (page == 1) {
                            conversationList.clear();
                            conversationList.addAll(newConversationList);
                        } else {
                            MyUtils.expandUnique(conversationList, newConversationList);

                            if (conversationList.size() == oldLen) {
                                canAutoLoad = false;
                            }
                        }
                        if (refresh) {
                            smoothToFirst.notifyChange();
                        }
                        MainActivityVM.this.page = page;
                    }
                }));
    }

    public void refresh(){
        page = 1;
        getList(page, true);
    }
    public void loadMore() {
        if (isRefreshing.get()) return;
        getList(page + 1);
    }

    /**
     * 去掉刷新时的圆圈
     */
    private void removeCircle() {
        isRefreshing.set(false);
        isRefreshing.notifyChange();
    }

    /**
     * 显示刷新时的圆圈
     */
    private void showCircle() {
        isRefreshing.set(true);
        isRefreshing.notifyChange();
    }

    public void onClickAvatar(View view) {
        if (isLoggedIn.get()) {
            goToHomepage.notifyChange();
        } else {
            goToLogin.notifyChange();
        }
    }

    public void onClickPostFab(View view) {
        goToPost.notifyChange();
    }

    public void onClickConversation(Conversation conversation) {
        Log.d(TAG, "onClickConversation: ");
        clickedConversation = conversation;
        goToConversation.notifyChange();
    }

    public void tryToLoadFromBottom() {
        if (canAutoLoad) {
            canAutoLoad = false;
            loadMore();
        }
    }

    public void startUp() {
        Log.d(TAG, "startUp() called");
        isRefreshing.set(true);

        if (LoginUtils.hasSavedData()) {
            isLoggedIn.set(true);   // isLoggedIn is only to determine whether there are available username, signature etc to show

            Me.setInstanceFromSharedPreference(ChaoliApplication.getAppContext(), LoginUtils.getSavedUsername());
            if (!Me.isEmpty()) {
                myUsername.set(LoginUtils.getSavedUsername());
                myAvatarSuffix.set(Me.getMyAvatarSuffix());
                mySignature.set(Me.getMySignature());
            } else {
                myUsername.set(getString(R.string.loading));
                mySignature.set(getString(R.string.loading));
            }
        }

        LoginUtils.begin_login(new LoginUtils.LoginObserver() {
            @Override
            public void onLoginSuccess(int userId, String token) {
                failed.set(false);
                isLoggedIn.set(true);
                loginComplete.set(true);

                Log.d(TAG, "onLoginSuccess: success");
                getProfile();

                if (channel.equals("") || channel.equals("all")) {
                    toFirstLoadConversation.notifyChange();   // update conversations
                }
                intervalSend();
            }

            @Override
            public void onLoginFailure(int statusCode) {
                if (statusCode == LoginUtils.EMPTY_UN_OR_PW) {
                    if (channel.equals("") || channel.equals("all")) {
                        toFirstLoadConversation.notifyChange();   // update conversations
                    }
                    failed.set(false);
                    isLoggedIn.set(false);
                    loginComplete.set(true);
                } else {
                    /**
                     * show error screen here
                     */
                    failed.set(true);
                    isRefreshing.set(false);
                    // no isLoggedIn.set(false) here, show name, signature in drawer
                    loginComplete.set(true);
                    toastContent = getString(R.string.network_err);
                    showToast.notifyChange();
                }
                Log.d(TAG, "onLoginFailure: " + statusCode);
            }
        });
    }

    public void getProfile() {
        Log.d(TAG, "getProfile() called, username = " + myUsername.get() + ", " + Me.getMyUsername());
        AccountUtils.getProfile(new AccountUtils.GetProfileObserver() {
            @Override
            public void onGetProfileSuccess() {
                myUserId.set(Me.getMyUserId());
                myUsername.set(Me.getMyUsername());
                myAvatarSuffix.set(Me.getMyAvatarSuffix());
                mySignature.set(Me.getMySignature());
            }

            @Override
            public void onGetProfileFailure() {

            }
        });
    }


    private void intervalSend(){
        subscription.add(Observable.interval(Constants.getNotificationInterval,
                Constants.getNotificationInterval,
                TimeUnit.SECONDS)
                .filter(new Func1<Long, Boolean>() {
                    @Override
                    public Boolean call(Long aLong) {
                        return !Me.isEmpty();
                    }
                }).flatMap(new Func1<Long, Observable<NotificationList>>() {
                    @Override
                    public Observable<NotificationList> call(Long aLong) {
                        return MyRetrofit.getService().checkNotification();
                    }
                }).filter(new Func1<NotificationList, Boolean>() {
                    @Override
                    public Boolean call(NotificationList notificationList) {
                        return notificationList != null;
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<NotificationList>() {
                    @Override
                    public void onCompleted() {
                        notificationsNum.set(RETURN_ERROR);
                    }

                    @Override
                    public void onError(Throwable e) {
                        notificationsNum.set(RETURN_ERROR);
                    }

                    @Override
                    public void onNext(NotificationList notificationList) {
                        notificationsNum.set(notificationList.count);
                    }
                }));
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getChannelByPosition(int position) {
        String[] channel =
                new String[]
                        {
                                "",
                                Channel.caff.name(),
                                Channel.maths.name(),
                                Channel.physics.name(),
                                Channel.chem.name(),
                                Channel.biology.name(),
                                Channel.tech.name(),
                                Channel.court.name(),
                                Channel.announ.name(),
                                Channel.others.name(),
                                Channel.socsci.name(),
                                Channel.lang.name(),
                        };
        return channel[position];
    }

    public void resume() {
        if(!Me.isEmpty()) {
            //timer.cancel();
            mySignature.set(Me.getMySignature());
            myAvatarSuffix.set(Me.getAvatarSuffix());
            intervalSend();
        }
    }

    public void destory() {
        MyOkHttp.getClient().dispatcher().cancelAll();
        subscription.clear();
    }
}
