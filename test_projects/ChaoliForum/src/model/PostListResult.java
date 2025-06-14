package com.daquexian.chaoli.forum.model;

import java.util.List;

/**
 * Created by jianhao on 16-8-25.
 */
public class PostListResult {
    Conversation conversation;
    List<Post> posts;

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public List<Post> getPosts() {
        return posts;
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts;
    }
}
