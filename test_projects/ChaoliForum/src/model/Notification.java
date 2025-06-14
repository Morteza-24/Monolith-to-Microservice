package com.daquexian.chaoli.forum.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by jianhao on 16-9-3.
 */
public class Notification {
    public String fromMemberId;
    public String fromMemberName;
    @SerializedName("avatarFormat")
    public String avatarSuffix;
    public Data data;
    public String type;

    public static class Data{
        public String conversationId;
        public String postId;
        public String title;
    }
}
