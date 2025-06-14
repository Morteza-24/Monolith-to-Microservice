package com.daquexian.chaoli.forum.binding;

import android.databinding.BindingAdapter;

import com.daquexian.chaoli.forum.meta.PostContentView;
import com.daquexian.chaoli.forum.model.Post;

/**
 * Created by jianhao on 16-9-27.
 */

public class PostContentViewBA {
    @BindingAdapter("app:post")
    public static void setPost(PostContentView postContentView, Post post) {
        postContentView.setPost(post);
    }

    @BindingAdapter("app:listener")
    public static void setListener(PostContentView postContentView, PostContentView.OnViewClickListener onViewClickListener) {
        postContentView.setOnImgClickListener(onViewClickListener);
    }
}