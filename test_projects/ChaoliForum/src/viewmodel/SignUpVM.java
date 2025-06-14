package com.daquexian.chaoli.forum.viewmodel;

import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.network.MyOkHttp;
import com.daquexian.chaoli.forum.utils.LoginUtils;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Response;

/**
 * Created by jianhao on 16-9-21.
 */

public class SignUpVM extends BaseViewModel {
    private static final String TAG = "SignUpVM";

    String inviteCode;
    String signUpUrl;
    String token;

    public ObservableField<String> username = new ObservableField<>("");
    public ObservableField<String> password = new ObservableField<>("");
    public ObservableField<String> confirm = new ObservableField<>("");
    public ObservableField<String> captcha = new ObservableField<>("");
    public ObservableField<String> email = new ObservableField<>("");
    public ObservableField<String> usernameError = new ObservableField<>("");
    public ObservableField<String> passwordError = new ObservableField<>("");
    public ObservableField<String> confirmError = new ObservableField<>("");
    public ObservableField<String> captchaError = new ObservableField<>("");
    public ObservableField<String> emailError = new ObservableField<>("");
    public ObservableField<Drawable> captchaImg = new ObservableField<>();

    public ObservableInt showToast = new ObservableInt();
    public ObservableField<String> toastContent = new ObservableField<>();
    public ObservableBoolean showProcessDialog = new ObservableBoolean();
    public ObservableBoolean signUpSuccess = new ObservableBoolean();

    public SignUpVM(String inviteCode) {
        this.inviteCode = inviteCode;
        signUpUrl = Constants.SIGN_UP_URL + inviteCode;
    }

    public void init() {
        MyOkHttp.clearCookie();
        new MyOkHttp.MyOkHttpClient()
                .get(signUpUrl)
                .enqueue(new MyOkHttp.Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        showToast.notifyChange();
                        toastContent.set(getString(R.string.network_err));
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        if (response.code() != 200) {
                            showToast.notifyChange();
                            toastContent.set(getString(R.string.network_err));
                        } else {
                            response.body().close();
                            String tokenFormat = "\"token\":\"([\\dabcdef]+)";
                            Pattern pattern = Pattern.compile(tokenFormat);
                            Matcher matcher = pattern.matcher(responseStr);
                            if (matcher.find()) {
                                token = matcher.group(1);
                                getAndShowCaptchaImage();
                            } else {
                                showToast.notifyChange();
                                toastContent.set(getString(R.string.network_err));
                            }
                        }
                    }
                });
    }
    private void getAndShowCaptchaImage(){
        captchaImg.set(ResourcesCompat.getDrawable(ChaoliApplication.getAppContext().getResources(), R.drawable.refreshing, null));
        new MyOkHttp.MyOkHttpClient()
                .get(Constants.GET_CAPTCHA_URL)
                .enqueue(new MyOkHttp.Callback1() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.d(TAG, "onFailure: ");
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        byte[] bytes = response.body().bytes();
                        captchaImg.set(new BitmapDrawable(ChaoliApplication.getAppContext().getResources(), BitmapFactory.decodeByteArray(bytes, 0, bytes.length)));
                        Log.d(TAG, "onResponse: ");
                    }
                });
    }

    public void clickSignUp() {
        final String USERNAME_HAS_BEEN_USED = "用户名已经被注册了";
        final String EMAIL_HAS_BEEN_USED = "邮箱已被注册";
        final String WRONG_CAPTCHA = "你也许需要一个计算器";

        Boolean flagError = false;

        if (username.get().length() < 4 || username.get().length() > 21) {
            usernameError.set(getString(R.string.length_of_username_should_be_between_4_and_21));
            flagError = true;
        }
        if (password.get().length() < 6) {
            passwordError.set(getString(R.string.at_least_six_character));
            flagError = true;
        }
        if (!password.get().equals(confirm.get())) {
            confirmError.set(getString(R.string.should_be_same_with_password));
            flagError = true;
        }
        if (!email.get().contains("@") || !email.get().contains(".")) {
            emailError.set(getString(R.string.invaild_email));
            flagError = true;
        }
        if (flagError) return;

        showProcessDialog.set(true);

        MyOkHttp.MyOkHttpClient myOkHttpClient = new MyOkHttp.MyOkHttpClient()
                .add("username", username.get())
                .add("email", email.get())
                .add("password", password.get())
                .add("confirm", confirm.get())
                .add("mscaptcha", captcha.get())
                .add("token", token)
                .add("submit", "注册");

        myOkHttpClient.post(signUpUrl)
                .enqueue(new MyOkHttp.Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        toastContent.set(getString(R.string.network_err));
                        showToast.notifyChange();
                        getAndShowCaptchaImage();
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        if (response.code() != 200){
                            toastContent.set(getString(R.string.network_err));
                            showToast.notifyChange();
                            getAndShowCaptchaImage();
                            showProcessDialog.set(false);
                        }
                        else {
                            response.body().close();
                            if(responseStr.contains(USERNAME_HAS_BEEN_USED)){
                                usernameError.set(getString(R.string.username_has_been_used));
                            }else if(responseStr.contains(EMAIL_HAS_BEEN_USED)){
                                emailError.set(getString(R.string.email_has_been_used));
                            } else if (responseStr.contains(WRONG_CAPTCHA)) {
                                captchaError.set(getString(R.string.wrong_captcha));
                            } else {
                                /**
                                 * success
                                 */
                                Log.d(TAG, "onResponse: " + responseStr);
                                ChaoliApplication.getSp().edit().remove(Constants.INVITING_CODE_SP).apply();
                                LoginUtils.saveUsernameAndPasswordToSp(username.get(), password.get());
                                signUpSuccess.notifyChange();
                            }
                            showProcessDialog.set(false);
                        }

                    }
                });
    }

    public void clickRefreshCaptcha() {
        getAndShowCaptchaImage();
    }
}
