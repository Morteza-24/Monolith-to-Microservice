package com.daquexian.chaoli.forum.model;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.PropertyChangeRegistry;
import android.os.Parcel;
import android.os.Parcelable;

import com.daquexian.chaoli.forum.BR;
import com.daquexian.chaoli.forum.binding.DiffItem;
import com.google.gson.annotations.SerializedName;

public class Conversation extends BaseObservable implements DiffItem, Comparable<Conversation>, Parcelable {
	private static final String TAG = "Conversation";
	private int conversationId;
	private int channelId;
	private String title;
	private String firstPost;
	private String link;
	private String startMemberId;
	private String lastPostMemberId;
	private String startMember;
	@SerializedName("startMemberAvatarFormat")
	private String startMemberAvatarSuffix;
	private String startTime;
	@SerializedName("lastPostMemberAvatarFormat")
	private String lastPostMemberAvatarSuffix;
	private String lastPostMember;
	private String lastPostTime;
	private String unread;
	private int replies;
	private transient PropertyChangeRegistry propertyChangeRegistry = new PropertyChangeRegistry();


	@Override
	public boolean areContentsTheSame(DiffItem anotherItem) {
		Conversation newConversation = (Conversation) anotherItem;
		return !(this.getFirstPost() == null && newConversation.getFirstPost() != null)
				&& !(this.getFirstPost() != null && newConversation.getFirstPost() == null)
				&& ((this.getFirstPost() == null && newConversation.getFirstPost() == null) || this.getFirstPost().equals(newConversation.getFirstPost()))
				&& this.getReplies() == newConversation.getReplies();
	}

	@Override
	public boolean areItemsTheSame(DiffItem anotherItem) {
		Conversation newConversation = (Conversation) anotherItem;
		return this.getConversationId() == newConversation.getConversationId();
	}

	/**
	 * 对于主题帖来说，最新的排在最前，不同于单个主题帖中的楼层，最先发表的排在最前
	 *
	 * @param o 另一个主题帖
	 * @return 比较结果
	 */
	@Override
	public int compareTo(Conversation o) {
		return o.getLastPostTime().compareTo(getLastPostTime());
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(this.conversationId);
		dest.writeInt(this.channelId);
		dest.writeString(this.title);
		dest.writeString(this.firstPost);
		dest.writeString(this.link);
		dest.writeString(this.startMemberId);
		dest.writeString(this.lastPostMemberId);
		dest.writeString(this.startMember);
		dest.writeString(this.startMemberAvatarSuffix);
		dest.writeString(this.lastPostMemberAvatarSuffix);
		dest.writeString(this.lastPostMember);
		dest.writeString(this.lastPostTime);
		dest.writeInt(this.replies);
		dest.writeString(this.startTime);
		dest.writeString(this.unread);
	}

	public Conversation() {
		replies = -1;
	}

	protected Conversation(Parcel in) {
		this.conversationId = in.readInt();
		this.channelId = in.readInt();
		this.title = in.readString();
		this.firstPost = in.readString();
		this.link = in.readString();
		this.startMemberId = in.readString();
		this.lastPostMemberId = in.readString();
		this.startMember = in.readString();
		this.startMemberAvatarSuffix = in.readString();
		this.lastPostMemberAvatarSuffix = in.readString();
		this.lastPostMember = in.readString();
		this.lastPostTime = in.readString();
		this.replies = in.readInt();
		this.startTime = in.readString();
		this.unread = in.readString();
	}

	public static final Creator<Conversation> CREATOR = new Creator<Conversation>() {
		@Override
		public Conversation createFromParcel(Parcel source) {
			return new Conversation(source);
		}

		@Override
		public Conversation[] newArray(int size) {
			return new Conversation[size];
		}
	};

	@Bindable
	public int getConversationId() {
		return conversationId;
	}

	public void setConversationId(int conversationId) {
		this.conversationId = conversationId;
		notifyChange(BR.conversationId);
	}

	@Bindable
	public int getChannelId() {
		return channelId;
	}

	public void setChannelId(int channelId) {
		this.channelId = channelId;
		notifyChange(BR.channelId);
	}

	@Bindable
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
		notifyChange(BR.title);
	}

	@Bindable
	public String getFirstPost() {
		return firstPost;
	}

	public void setFirstPost(String firstPost) {
		this.firstPost = firstPost;
		notifyChange(BR.firstPost);
	}

	@Bindable
	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
		notifyChange(BR.link);
	}

	@Bindable
	public String getStartMemberId() {
		return startMemberId;
	}

	public void setStartMemberId(String startMemberId) {
		this.startMemberId = startMemberId;
		notifyChange(BR.startMemberId);
	}

	@Bindable
	public String getLastPostMemberId() {
		return lastPostMemberId;
	}

	public void setLastPostMemberId(String lastPostMemberId) {
		this.lastPostMemberId = lastPostMemberId;
		notifyChange(BR.lastPostMemberId);
	}

	@Bindable
	public String getStartMember() {
		return startMember;
	}

	public void setStartMember(String startMember) {
		this.startMember = startMember;
		notifyChange(BR.startMember);
	}

	@Bindable
	public String getStartMemberAvatarSuffix() {
		return startMemberAvatarSuffix;
	}

	public void setStartMemberAvatarSuffix(String startMemberAvatarSuffix) {
		this.startMemberAvatarSuffix = startMemberAvatarSuffix;
		notifyChange(BR.startMemberAvatarSuffix);
	}

	@Bindable
	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
		notifyChange(BR.startTime);
	}

	@Bindable
	public String getLastPostMemberAvatarSuffix() {
		return lastPostMemberAvatarSuffix;
	}

	public void setLastPostMemberAvatarSuffix(String lastPostMemberAvatarSuffix) {
		this.lastPostMemberAvatarSuffix = lastPostMemberAvatarSuffix;
		notifyChange(BR.lastPostMemberAvatarSuffix);
	}

	@Bindable
	public String getLastPostMember() {
		return lastPostMember;
	}

	public void setLastPostMember(String lastPostMember) {
		this.lastPostMember = lastPostMember;
		notifyChange(BR.lastPostMember);
	}

	@Bindable
	public String getLastPostTime() {
		return lastPostTime;
	}

	public void setLastPostTime(String lastPostTime) {
		this.lastPostTime = lastPostTime;
		notifyChange(BR.lastPostTime);
	}

	@Bindable
	public String getUnread() {
		return unread;
	}

	public void setUnread(String unread) {
		this.unread = unread;
		notifyChange(BR.unread);
	}

	@Bindable
	public int getReplies() {
		return replies;
	}

	public void setReplies(int replies) {
		this.replies = replies;
		notifyChange(BR.replies);
	}

	private void notifyChange(int propertyId) {
		if (propertyChangeRegistry == null) {
			propertyChangeRegistry = new PropertyChangeRegistry();
		}
		propertyChangeRegistry.notifyChange(this, propertyId);
	}

	@Override
	public void addOnPropertyChangedCallback(OnPropertyChangedCallback callback) {
		if (propertyChangeRegistry == null) {
			propertyChangeRegistry = new PropertyChangeRegistry();
		}
		propertyChangeRegistry.add(callback);

	}

	@Override
	public void removeOnPropertyChangedCallback(OnPropertyChangedCallback callback) {
		if (propertyChangeRegistry != null) {
			propertyChangeRegistry.remove(callback);
		}
	}
}
