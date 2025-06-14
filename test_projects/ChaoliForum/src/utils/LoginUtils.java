package com.daquexian.chaoli.forum.utils;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.data.Me;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.model.UserIdAndTokenResult;
import com.daquexian.chaoli.forum.network.MyOkHttp;
import com.daquexian.chaoli.forum.network.MyOkHttp.Callback;
import com.daquexian.chaoli.forum.network.MyRetrofit;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Response;

public class LoginUtils {
    @SuppressWarnings("unused")
    private static final String TAG = "LoginUtils";
    private static final String LOGIN_SP_NAME = "username_and_password";
    private static final String IS_LOGGED_IN = "is_logged_in";
    private static final String SP_USERNAME_KEY = "sUsername";
    private static final String SP_PASSWORD_KEY = "sPassword";
    public static final int FAILED_AT_OPEN_LOGIN_PAGE = 0;
    public static final int FAILED_AT_GET_TOKEN_ON_LOGIN_PAGE = 1;
    public static final int FAILED_AT_LOGIN = 2;
    public static final int WRONG_USERNAME_OR_PASSWORD = 3;
    public static final int FAILED_AT_OPEN_HOMEPAGE = 4;
    public static final int COOKIE_EXPIRED = 5;
    public static final int EMPTY_UN_OR_PW = 6;
    public static final int ERROR_LOGIN_STATUS = 7; // TODO: 17-1-2 handle this error

    private static void setToken(String token) {
        LoginUtils.sToken = token;
    }

    public static String getToken() {
        return sToken;
    }

    private static String sUsername;
    private static String sPassword;
    private static String sToken;
    private static boolean sIsLoggedIn;

    private static SharedPreferences sSharedPreferences;

    public static void begin_login(final String username, final String password, final LoginObserver loginObserver){
        sSharedPreferences = ChaoliApplication.getAppContext().getSharedPreferences(LOGIN_SP_NAME, Context.MODE_PRIVATE);

        Log.d("login", username + ", " + password);

        sUsername = username;
        sPassword = password;
        pre_login(loginObserver);
    }

    public static void begin_login(LoginObserver loginObserver){

        sSharedPreferences = ChaoliApplication.getAppContext().getSharedPreferences(LOGIN_SP_NAME, Context.MODE_PRIVATE);

        sUsername = sSharedPreferences.getString(SP_USERNAME_KEY, "");
        sPassword = sSharedPreferences.getString(SP_PASSWORD_KEY, "");

        if("".equals(sUsername) || "".equals(sPassword)){
            loginObserver.onLoginFailure(EMPTY_UN_OR_PW);
            return;
        }

        Log.d("login", sUsername + ", " + sPassword);

        begin_login(sUsername, sPassword, loginObserver);
    }

    private static void pre_login(final LoginObserver loginObserver){//获取登录页面的token
        MyRetrofit.getService().getUserIdAndToken()
                .enqueue(new retrofit2.Callback<UserIdAndTokenResult>() {
                    @Override
                    public void onResponse(retrofit2.Call<UserIdAndTokenResult> call, retrofit2.Response<UserIdAndTokenResult> response) {
                        if (response.body() == null) {
                            onFailure(call, new RuntimeException("poor network"));
                            return;
                        }

                        if (response.body().getUserId() == 0) {
                            setToken(response.body().getToken());
                            login(loginObserver);
                        } else {
                            saveUserInfo(response.body().getUserId(), sUsername, response.body().getToken());
                            loginObserver.onLoginSuccess(Me.getMyUserId(), getToken());
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<UserIdAndTokenResult> call, Throwable t) {
                        loginObserver.onLoginFailure(FAILED_AT_GET_TOKEN_ON_LOGIN_PAGE);
                    }
                });
    }

    private static void login(final LoginObserver loginObserver){ //发送请求登录
        MyRetrofit.getService()
                .login(sUsername, sPassword, getToken(), "user/login.json", "登录")
                .enqueue(new retrofit2.Callback<UserIdAndTokenResult>() {
                    @Override
                    public void onResponse(retrofit2.Call<UserIdAndTokenResult> call, retrofit2.Response<UserIdAndTokenResult> response) {
                        saveUserInfo(response.body().getUserId(), sUsername, response.body().getToken());

                        saveUsernameAndPasswordToSp(sUsername, sPassword);

                        loginObserver.onLoginSuccess(Me.getMyUserId(), getToken());
                    }

                    @Override
                    public void onFailure(retrofit2.Call<UserIdAndTokenResult> call, Throwable t) {
                        loginObserver.onLoginFailure(WRONG_USERNAME_OR_PASSWORD);
                        t.printStackTrace();
                    }
                });
    }

    public static void logout(final LogoutObserver logoutObserver){
        String logoutURL = Constants.LOGOUT_PRE_URL + getToken();
        clear(ChaoliApplication.getAppContext());
        Me.clear();
        new MyOkHttp.MyOkHttpClient()
                .get(logoutURL)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        logoutObserver.onLogoutFailure(0);
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        logoutObserver.onLogoutSuccess();
                    }
                });
    }

    public static void clear(Context context){
        MyOkHttp.clearCookie();
        sIsLoggedIn = false;
        sSharedPreferences = context.getSharedPreferences(LOGIN_SP_NAME, Context.MODE_PRIVATE);
        if(sSharedPreferences != null) {
            SharedPreferences.Editor editor = sSharedPreferences.edit();
            editor.remove(IS_LOGGED_IN);
            editor.remove(SP_USERNAME_KEY);
            editor.remove(SP_PASSWORD_KEY);
            editor.apply();
        }
    }

    public static boolean isLoggedIn() {
        return sIsLoggedIn;
    }

    public static boolean hasSavedData() {
        sSharedPreferences = ChaoliApplication.getAppContext().getSharedPreferences(LOGIN_SP_NAME, Context.MODE_PRIVATE);
        return !sSharedPreferences.getString(SP_USERNAME_KEY, "").equals("") && !sSharedPreferences.getString(SP_PASSWORD_KEY, "").equals("");
    }

    public static String getSavedUsername() {
        sSharedPreferences = ChaoliApplication.getAppContext().getSharedPreferences(LOGIN_SP_NAME, Context.MODE_PRIVATE);
        return sSharedPreferences.getString(SP_USERNAME_KEY, "");
    }

    public static void saveUsernameAndPasswordToSp(String username, String password) {
        sSharedPreferences = ChaoliApplication.getAppContext().getSharedPreferences(LOGIN_SP_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sSharedPreferences.edit();
        editor.putString(SP_USERNAME_KEY, username);
        // TODO: 16-3-11 1915 Encrypt saved sPassword
        editor.putString(SP_PASSWORD_KEY, password);
        editor.apply();
    }

    private static void saveUserInfo(int userId, String username, String token) {
        Me.setUserId(userId);
        Me.setUsername(username);
        setToken(token);
        sIsLoggedIn = true;
    }
    public interface LoginObserver
    {
        void onLoginSuccess(int userId, String token);
        void onLoginFailure(int statusCode);
    }

    public interface LogoutObserver
    {
        void onLogoutSuccess();
        void onLogoutFailure(int statusCode);
    }
}
