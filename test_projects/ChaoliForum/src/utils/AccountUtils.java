package com.daquexian.chaoli.forum.utils;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.data.Me;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.model.NotificationList;
import com.daquexian.chaoli.forum.model.User;
import com.daquexian.chaoli.forum.network.MyOkHttp;
import com.daquexian.chaoli.forum.network.MyRetrofit;

import java.io.File;
import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by daquexian on 16-3-17.
 * 和账户相关的类，包括获取自己的用户信息、检查是否帖子更新、是否有新动态及更改账户设置
 */
public class AccountUtils {
    @SuppressWarnings("unused")
    private static final String TAG = "AccountUtils";

    public static void getProfile(final GetProfileObserver observer){
        MyRetrofit.getService().getProfile()
                .enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call, Response<User> response) {
                        Me.setProfile(ChaoliApplication.getAppContext(), response.body());
                        observer.onGetProfileSuccess();
                    }

                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        observer.onGetProfileFailure();
                    }
                });
    }

    public static void modifySettings(File avatar, String language, Boolean privateAdd, Boolean starOnReply, Boolean starPrivate, Boolean hideOnline,
                                      final String signature, String userStatus, final ModifySettingsObserver observer){
        MyOkHttp.MyOkHttpClient myOkHttpClient = new MyOkHttp.MyOkHttpClient()
                .add("token", LoginUtils.getToken())
                .add("language", language)
                .add("userStatus", userStatus)
                .add("signature", signature);
        myOkHttpClient.add("avatar", "image/*", avatar);
        if(hideOnline) myOkHttpClient.add("hideOnline", hideOnline.toString());
        if(starPrivate) myOkHttpClient.add("starPrivate", starPrivate.toString());
        if(starOnReply) myOkHttpClient.add("starOnReply", starOnReply.toString());
        if(privateAdd) myOkHttpClient.add("privateAdd", privateAdd.toString());
        myOkHttpClient.add("save", "保存更改");
        myOkHttpClient
                .post(Constants.MODIFY_SETTINGS_URL)
                .enqueue(new MyOkHttp.Callback() {
                    @Override
                    public void onFailure(okhttp3.Call call, IOException e) {
                        observer.onModifySettingsFailure(-3);
                    }

                    @Override
                    public void onResponse(okhttp3.Call call, okhttp3.Response response, String responseStr) throws IOException {
                        observer.onModifySettingsSuccess();
                    }
                });
    }


    /* 生成类似 1,2,3, 格式的字符串 */
    @SuppressWarnings("unused")
    private static String intJoin(int[] aArr, String sSep) {
        StringBuilder sbStr = new StringBuilder();
        for (int i:
                aArr) {
            sbStr.append(i);
            sbStr.append(sSep);
        }
        return sbStr.toString();
    }

    public interface ModifySettingsObserver{
        void onModifySettingsSuccess();
        void onModifySettingsFailure(int statusCode);
    }

    public interface MessageObserver {
        void onCheckNotificationSuccess(NotificationList notificationList);
        void onCheckNotificationFailure(int statusCode);
    }

    public interface GetProfileObserver{
        void onGetProfileSuccess();
        void onGetProfileFailure();
    }
}
