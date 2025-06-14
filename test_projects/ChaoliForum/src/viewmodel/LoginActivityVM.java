package com.daquexian.chaoli.forum.viewmodel;

import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.utils.LoginUtils;

/**
 * ViewModel for LoginActivity
 * Created by jianhao on 16-9-21.
 */

public class LoginActivityVM extends BaseViewModel {
    public ObservableField<String> username = new ObservableField<>();
    public ObservableField<String> password = new ObservableField<>();

    public ObservableInt showToast = new ObservableInt();
    public String toastContent;
    public ObservableBoolean showProgressDialog = new ObservableBoolean();
    public ObservableInt goToMainActivity = new ObservableInt();
    public ObservableInt clickAQ = new ObservableInt();
    public ObservableInt clickSignUp = new ObservableInt();

    public void clickLogin() {
        if ("".equals(username.get()) || "".equals(password.get())) {
            toastContent = "".equals(username.get()) ? getString(R.string.username)
                    + ("".equals(password.get()) ? " " + getString(R.string.and_password) : "") : getString(R.string.password);
            showToast.notifyChange();
        } else {
            showProgressDialog.set(true);
            LoginUtils.LoginObserver observer = new LoginUtils.LoginObserver() {
                @Override
                public void onLoginSuccess(int userId, String token) {
                    showProgressDialog.set(false);
                    goToMainActivity.notifyChange();
                }

                @Override
                public void onLoginFailure(int statusCode) {
                    showProgressDialog.set(false);
                    switch (statusCode) {
                        case LoginUtils.FAILED_AT_OPEN_LOGIN_PAGE:
                            toastContent = getString(R.string.network_err_open_login_page);
                            break;
                        case LoginUtils.FAILED_AT_GET_TOKEN_ON_LOGIN_PAGE:
                            toastContent = getString(R.string.network_err_get_token);
                            break;
                        case LoginUtils.FAILED_AT_LOGIN:
                            toastContent = getString(R.string.network_err_login);
                            break;
                        case LoginUtils.WRONG_USERNAME_OR_PASSWORD:
                            toastContent = getString(R.string.login_err_wrong_name_pwd);
                            break;
                        case LoginUtils.FAILED_AT_OPEN_HOMEPAGE:
                            toastContent = getString(R.string.network_err_homepage);
                            break;
                        case LoginUtils.COOKIE_EXPIRED:
                            toastContent = getString(R.string.login_err_cookie_expire);
                            break;
                        case LoginUtils.EMPTY_UN_OR_PW:
                            toastContent = getString(R.string.login_err_empty);
                            break;
                        case LoginUtils.ERROR_LOGIN_STATUS:
                            toastContent = getString(R.string.try_again);
                            break;
                    }
                    showToast.notifyChange();
                    LoginUtils.clear(ChaoliApplication.getAppContext());
                }
            };
            LoginUtils.begin_login(username.get(), password.get(), observer);
        }
    }

    public void clickSignUp() {
        clickSignUp.set(clickSignUp.get() + 1);
    }

    public void clickAnswerQuestion() {
        clickAQ.set(clickAQ.get() + 1);
    }
}
