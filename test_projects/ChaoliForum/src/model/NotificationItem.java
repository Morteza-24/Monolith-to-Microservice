package com.daquexian.chaoli.forum.model;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.viewmodel.HistoryFragmentVM;
import com.google.gson.annotations.SerializedName;

/**
 * Created by jianhao on 16-9-3.
 */
public class NotificationItem extends HistoryFragmentVM.ListItem {
    public static final String TYPE_MENTION        = "mention";
    public static final String TYPE_POST           = "post";
    public static final String TYPE_PRIVATE_ADD    = "privateAdd";

    String fromMemberId;
    String fromMemberName;

    public String getFromMemberId() {
        return fromMemberId;
    }

    public void setFromMemberId(String fromMemberId) {
        this.fromMemberId = fromMemberId;
    }

    public String getFromMemberName() {
        return fromMemberName;
    }

    public void setFromMemberName(String fromMemberName) {
        this.fromMemberName = fromMemberName;
    }

    public Boolean getUnread() {
        return unread;
    }

    public void setUnread(Boolean unread) {
        this.unread = unread;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAvatarSuffix() {
        return avatarSuffix;
    }

    public void setAvatarSuffix(String avatarSuffix) {
        this.avatarSuffix = avatarSuffix;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    Boolean unread;
    String content;
    @SerializedName("avatarFormat")
    String avatarSuffix;
    Data data;

    public static class Data{
        String conversationId;
        String postId;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getConversationId() {
            return conversationId;
        }

        public void setConversationId(String conversationId) {
            this.conversationId = conversationId;
        }

        public String getPostId() {
            return postId;
        }

        public void setPostId(String postId) {
            this.postId = postId;
        }

        String title;
    }

    @Override
    public int getShowingPostId() {
        return getData().getPostId() != null ? Integer.parseInt(getData().getPostId()) : -1;
    }

    @Override
    public int getAvatarUserId() {
        return Integer.parseInt(getFromMemberId());
    }

    @Override
    public String getShowingTitle() {
        switch (getType()) {
            case NotificationItem.TYPE_MENTION:
                //title.set(ChaoliApplication.getAppContext().getString(R.string.mention_you, notificationItem.getFromMemberName()));
                return ChaoliApplication.getAppContext().getString(R.string.mention_you, getFromMemberName());
            case NotificationItem.TYPE_POST:
                //title.set(ChaoliApplication.getAppContext().getString(R.string.updated, notificationItem.getFromMemberName()));
                return ChaoliApplication.getAppContext().getString(R.string.updated, getFromMemberName());
            case NotificationItem.TYPE_PRIVATE_ADD:
                //title.set(ChaoliApplication.getAppContext().getString(R.string.send_you_a_private_post, notificationItem.getFromMemberName()));
                return ChaoliApplication.getAppContext().getString(R.string.send_you_a_private_post, getFromMemberName());
            default:
                return null;
        }
    }

    @Override
    public String getAvatarUsername() {
        return getFromMemberName();
    }

    @Override
    public int getConversationId() {
        switch (getType()) {
            case NotificationItem.DIVIDER:
            case NotificationItem.SPACE:
                return 0;
            default:
                return Integer.parseInt(getData().getConversationId());
        }
    }

    @Override
    public String getShowingContent() {
        return getData().getTitle();
    }
}
