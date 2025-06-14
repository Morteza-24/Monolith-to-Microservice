package com.daquexian.chaoli.forum.viewmodel;

import android.content.SharedPreferences;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.util.Log;

import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.data.Me;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.utils.AccountUtils;

import java.io.File;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by jianhao on 16-10-6.
 */

public class SettingsVM extends BaseViewModel {
    private static final String TAG = "SVM";

    public ObservableBoolean showProcessDialog = new ObservableBoolean();
    public String dialogContent;
    public ObservableInt showToast = new ObservableInt();
    public ObservableField<String> toastContent = new ObservableField<>();
    public ObservableInt goToAlbum = new ObservableInt();
    public ObservableBoolean complete = new ObservableBoolean();

    public ObservableField<String> signature = new ObservableField<>();
    public ObservableField<String> userStatus = new ObservableField<>();
    public ObservableBoolean privateAdd = new ObservableBoolean();
    public ObservableBoolean starOnReply = new ObservableBoolean();
    public ObservableBoolean starPrivate = new ObservableBoolean();
    public ObservableBoolean hideOnline = new ObservableBoolean();
    public File avatarFile;

    /* App Settings */
    public ObservableBoolean clickTwiceToExit = new ObservableBoolean();

    private SharedPreferences sharedPreferences;

    public SettingsVM(String signature, String userStatus, Boolean privateAdd, Boolean starOnReply, Boolean starPrivate, Boolean hideOnline) {
        this.signature.set(signature);
        this.userStatus.set(userStatus);
        this.privateAdd.set(privateAdd);
        this.starOnReply.set(starOnReply);
        this.starPrivate.set(starPrivate);
        this.hideOnline.set(hideOnline);

        sharedPreferences = getSharedPreferences(Constants.SETTINGS_SP, MODE_PRIVATE);
        clickTwiceToExit.set(sharedPreferences.getBoolean(Constants.CLICK_TWICE_TO_EXIT, false));
    }

    public void save() {
        dialogContent = getString(R.string.just_a_sec);
        showProcessDialog.set(true);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(Constants.CLICK_TWICE_TO_EXIT, clickTwiceToExit.get());
        editor.apply();
        AccountUtils.modifySettings(avatarFile, "Chinese", privateAdd.get(), starOnReply.get(),
                starPrivate.get(), hideOnline.get(), signature.get(), userStatus.get(), new AccountUtils.ModifySettingsObserver() {
                    @Override
                    public void onModifySettingsSuccess() {
                        AccountUtils.getProfile(new AccountUtils.GetProfileObserver() {
                            @Override
                            public void onGetProfileSuccess() {
                                Log.d(TAG, "onGetProfileSuccess: hi");
                                showProcessDialog.set(false);
                                dialogContent = getString(R.string.retrieving_new_data);
                                showProcessDialog.set(true);
                                // TODO: 16-11-16 adjust it with RxJava and Retrofit
                                AccountUtils.getProfile(new AccountUtils.GetProfileObserver() {
                                    @Override
                                    public void onGetProfileSuccess() {
                                        showProcessDialog.set(false);
                                        toastContent.set(getString(R.string.modified_successfully));
                                        showToast.notifyChange();
                                        complete.notifyChange();
                                    }

                                    @Override
                                    public void onGetProfileFailure() {
                                        showProcessDialog.set(false);
                                        toastContent.set(getString(R.string.network_err));
                                        showToast.notifyChange();
                                    }
                                });
                            }

                            @Override
                            public void onGetProfileFailure() {
                                showProcessDialog.set(false);
                                toastContent.set(getString(R.string.network_err));
                                showToast.notifyChange();
                            }
                        });
                    }

                    @Override
                    public void onModifySettingsFailure(int statusCode) {
                        showProcessDialog.set(false);
                        toastContent.set(getString(R.string.fail_on_modifying));
                        showToast.notifyChange();
                    }
                });
    }

    public void clickChangeAvatar() {
        goToAlbum.notifyChange();
    }

}
