package com.daquexian.chaoli.forum.model;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.viewmodel.HistoryFragmentVM;
import com.google.gson.annotations.SerializedName;

/**
 * 存储个人主页中的历史活动的类
 * Created by jianhao on 16-9-3.
 */
public class HistoryItem extends HistoryFragmentVM.ListItem {
    public static final String POST_ACTIVITY  = "postActivity";
    public static final String STATUS         = "status";
    public static final String JOIN           = "join";

    String start;
    String postId;
    @SerializedName("avatarFormat")
    String avatarSuffix;
    int fromMemberId;
    String fromMemberName;

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    String content;
    String title;
    String description;
    Data data;

    public static class Data{
        public String getNewStatus() {
            return newStatus;
        }

        public void setNewStatus(String newStatus) {
            this.newStatus = newStatus;
        }

        public String getNewSignature() {
            return newSignature;
        }

        public void setNewSignature(String newSignature) {
            this.newSignature = newSignature;
        }

        String newStatus;
        String newSignature;
    }

    @Override
    public String getAvatarUsername() {
        return fromMemberName;
    }

    @Override
    public int getConversationId() {
        return 0;
    }

    @Override
    public int getAvatarUserId() {
        return fromMemberId;
    }

    @Override
    public String getAvatarSuffix() {
        return avatarSuffix;
    }

    @Override
    public int getShowingPostId() {
        return getPostId() != null ? Integer.valueOf(getPostId()) : 0;
    }

    @Override
    public String getShowingTitle() {
        switch (getType()) {
            case POST_ACTIVITY:
                if ("1".equals(getStart())) return ChaoliApplication.getAppContext().getString(R.string.opened_a_conversation);
                else return ChaoliApplication.getAppContext().getString(R.string.updated, getTitle());
            case STATUS:
                return ChaoliApplication.getAppContext().getString(R.string.modified_his_or_her_information);
            case JOIN:
                return ChaoliApplication.getAppContext().getString(R.string.join_the_forum);
        }
        return "";
    }

    @Override
    public String getShowingContent() {
        switch (getType()) {
            case POST_ACTIVITY:
                return "1".equals(getStart()) ? getTitle() : getContent();
            case STATUS:
                if (data != null && data.getNewStatus() != null) return data.getNewStatus();
                return "";
            case JOIN:
                return "";
        }
        return "";
    }
}
