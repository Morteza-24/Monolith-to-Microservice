package com.daquexian.chaoli.forum.binding;

import android.databinding.BindingAdapter;

import com.daquexian.chaoli.forum.meta.AvatarView;

/**
 * Created by jianhao on 16-9-19.
 */
public class AvatarViewBA {
    @BindingAdapter({"bind:imageSuffix", "bind:userId", "bind:username"})
    public static void loadImage(AvatarView avatarView, String imageSuffix, int userId, String username) {
        if (userId != -1) avatarView.update(imageSuffix, userId, username);
    }

    @BindingAdapter({"bind:imageSuffix", "bind:userId", "bind:username", "app:login"})
    public static void loadImage(AvatarView avatarView, String imageSuffix, int userId, String username, Boolean login) {
        if (login) {
            loadImage(avatarView, imageSuffix, userId, username);
        } else {
            avatarView.setLoginImage(avatarView.getContext());
        }
    }

    @BindingAdapter("bind:length")
    public static void scale(AvatarView avatarView, int length) {
        avatarView.scale(length);
    }

    @BindingAdapter("bind:isLoggedIn")
    public static void loadLoginImage(AvatarView avatarView, Boolean login) {
        if (!login) avatarView.setLoginImage(avatarView.getContext());
    }
}
