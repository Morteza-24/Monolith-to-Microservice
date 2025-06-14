package com.daquexian.chaoli.forum.viewmodel;

import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;

import com.daquexian.chaoli.forum.data.Me;

/**
 * Created by jianhao on 16-9-21.
 */

public class HomepageVM extends BaseViewModel {
    public ObservableField<String> username = new ObservableField<>();
    public ObservableField<String> signature = new ObservableField<>();
    public ObservableField<String> avatarSuffix = new ObservableField<>();
    public ObservableInt userId = new ObservableInt();
    public ObservableBoolean isSelf = new ObservableBoolean();

    public HomepageVM(String username, String signature, String avatarSuffix, int userId, Boolean isSelf) {
        this.username.set(username);
        this.userId.set(userId);
        this.signature.set(signature);
        this.avatarSuffix.set(avatarSuffix);
        this.isSelf.set(isSelf);
    }

    public void updateSelfProfile() {
        signature.set(Me.getMySignature());
        avatarSuffix.set(Me.getAvatarSuffix());
    }
}
