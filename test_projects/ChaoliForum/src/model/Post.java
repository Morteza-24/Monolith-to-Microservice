package com.daquexian.chaoli.forum.model;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.support.annotation.Nullable;

import com.daquexian.chaoli.forum.BR;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Post extends BaseObservable implements Comparable<Post>
{
	public int postId;
	public int conversationId;
	public int memberId;
	public long time;
	public int editMemberId;
	public long editTime;
	public int deleteMemberId;
	public long deleteTime;
	public String title;
	public String content;
	public int floor;
	public String username;
	public String avatarFormat;
	public String signature;
	public List<Attachment> attachments = new ArrayList<>();

	public Post(){}
	public Post(int memberId, String username, String avatarSuffix, String content, String time) {
		this.memberId = memberId;
		this.username = username;
		this.avatarFormat = avatarSuffix;
		this.content = content;
		this.time = Long.parseLong(time);
		this.floor = 1;
	}

	public Post(int postId, int conversationId,
				int memberId, long time,
				int editMemberId, long editTime,
				int deleteMemberId, long deleteTime,
				String title, String content,
				int floor,
				String username, String avatarFormat,
				@Nullable Map<Integer, String> groups, @Nullable String groupNames,
				@Nullable String signature,
				@Nullable List<Attachment> attachments)
	{
		this.postId = postId;
		notifyPropertyChanged(BR.postId);
		this.conversationId = conversationId;
		notifyPropertyChanged(BR.conversationId);
		this.memberId = memberId;
		notifyPropertyChanged(BR.memberId);
		this.time = time;
		notifyPropertyChanged(BR.time);
		this.editMemberId = editMemberId;
		notifyPropertyChanged(BR.editMemberId);
		this.editTime = editTime;
		notifyPropertyChanged(BR.editTime);
		this.deleteMemberId = deleteMemberId;
		notifyPropertyChanged(BR.deleteMemberId);
		this.deleteTime = deleteTime;
		notifyPropertyChanged(BR.deleteTime);
		this.title = title;
		notifyPropertyChanged(BR.title);
		this.content = content;
		notifyPropertyChanged(BR.content);
		this.floor = floor;
		notifyPropertyChanged(BR.floor);
		this.username = username;
		notifyPropertyChanged(BR.username);
		this.avatarFormat = avatarFormat;
		notifyPropertyChanged(BR.avatarFormat);
		this.attachments = attachments;
		notifyPropertyChanged(BR.attachments);
	}

	@Bindable
	public int getPostId()
	{
		return postId;
	}

	public void setPostId(int postId)
	{
		this.postId = postId;
		notifyPropertyChanged(BR.postId);
	}

	@Bindable
	public int getConversationId()
	{
		return conversationId;
	}

	public void setConversationId(int conversationId)
	{
		this.conversationId = conversationId;
		notifyPropertyChanged(BR.conversationId);
	}

	@Bindable
	public int getMemberId()
	{
		return memberId;
	}

	public void setMemberId(int memberId)
	{
		this.memberId = memberId;
		notifyPropertyChanged(BR.memberId);
	}

	@Bindable
	public long getTime()
	{
		return time;
	}

	public void setTime(long time)
	{
		this.time = time;
		notifyPropertyChanged(BR.time);
	}

	@Bindable
	public int getEditMemberId()
	{
		return editMemberId;
	}

	public void setEditMemberId(int editMemberId)
	{
		this.editMemberId = editMemberId;
		notifyPropertyChanged(BR.editMemberId);
	}

	@Bindable
	public long getEditTime()
	{
		return editTime;
	}

	public void setEditTime(long editTime)
	{
		this.editTime = editTime;
		notifyPropertyChanged(BR.editTime);
	}

	@Bindable
	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
		notifyPropertyChanged(BR.signature);
	}

	@Bindable
	public int getDeleteMemberId()
	{
		return deleteMemberId;
	}

	public void setDeleteMemberId(int deleteMemberId)
	{
		this.deleteMemberId = deleteMemberId;
		notifyPropertyChanged(BR.deleteMemberId);
	}

	@Bindable
	public long getDeleteTime()
	{
		return deleteTime;
	}

	public void setDeleteTime(long deleteTime)
	{
		this.deleteTime = deleteTime;
		notifyPropertyChanged(BR.deleteTime);
	}

	@Bindable
	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
		notifyPropertyChanged(BR.title);
	}

	@Bindable
	public String getContent()
	{
		return content;
	}

	public void setContent(String content)
	{
		this.content = content;
		notifyPropertyChanged(BR.content);
	}

	@Bindable
	public int getFloor()
	{
		return floor;
	}

	public void setFloor(int floor)
	{
		this.floor = floor;
		notifyPropertyChanged(BR.floor);
	}

	@Bindable
	public String getUsername()
	{
		return username;
	}

	public void setUsername(String username)
	{
		this.username = username;
		notifyPropertyChanged(BR.username);
	}

	@Bindable
	public String getAvatarFormat()
	{
		return avatarFormat;
	}

	public void setAvatarFormat(String avatarFormat)
	{
		this.avatarFormat = avatarFormat;
		notifyPropertyChanged(BR.avatarFormat);
	}

	@Bindable
	public List<Attachment> getAttachments()
	{
		return attachments;
	}

	public void setAttachments(List<Attachment> attachments)
	{
		this.attachments = attachments;
		notifyPropertyChanged(BR.attachments);
	}

	/*public void setAvatarView(AvatarView avatarView)
	{
		this.avatarView = avatarView;
notifyPropertyChanged(BR.avatarView);
	}

	public void setAvatarView()
	{
		this.avatarView = new AvatarView(context, avatarFormat, memberId, username);
notifyPropertyChanged(BR.new AvatarView(context, avatarFormat, memberId, username));
	}*/

	/*public AvatarView getAvatarView()
	{
		return avatarView;
	}*/

	public class Attachment extends BaseObservable
	{
		public String attachmentId;
		public String filename;
		public String secret;
		public int postId;
		public int draftMemberId;
		public int draftConversationId;

		public Attachment()
		{
			this("", "", "", 0);
		}

		public Attachment(String attachmentId, String filename, String secret, int postId)
		{
			this(attachmentId, filename, secret, postId, 0, 0);
		}

		public Attachment(String attachmentId, String filename, String secret, int postId,
						  int draftMemberId, int draftConversationId)
		{
			this.attachmentId = attachmentId;
			notifyPropertyChanged(BR.attachmentId);
			this.filename = filename;
			notifyPropertyChanged(BR.filename);
			this.secret = secret;
			notifyPropertyChanged(BR.secret);
			this.postId = postId;
			notifyPropertyChanged(BR.postId);
			this.draftMemberId = draftMemberId;
			notifyPropertyChanged(BR.draftMemberId);
			this.draftConversationId = draftConversationId;
			notifyPropertyChanged(BR.draftConversationId);
		}

		@Bindable
		public String getAttachmentId()
		{
			return attachmentId;
		}

		public void setAttachmentId(String attachmentId)
		{
			this.attachmentId = attachmentId;
			notifyPropertyChanged(BR.attachmentId);
		}

		@Bindable
		public String getFilename()
		{
			return filename.toLowerCase();
		}

		public void setFilename(String filename)
		{
			this.filename = filename;
			notifyPropertyChanged(BR.filename);
		}

		@Bindable
		public String getSecret()
		{
			return secret;
		}

		public void setSecret(String secret)
		{
			this.secret = secret;
			notifyPropertyChanged(BR.secret);
		}

		@Bindable
		public int getPostId()
		{
			return postId;
		}

		public void setPostId(int postId)
		{
			this.postId = postId;
			notifyPropertyChanged(BR.postId);
		}

		@Bindable
		public int getDraftMemberId()
		{
			return draftMemberId;
		}

		public void setDraftMemberId(int draftMemberId)
		{
			this.draftMemberId = draftMemberId;
			notifyPropertyChanged(BR.draftMemberId);
		}

		@Bindable
		public int getDraftConversationId()
		{
			return draftConversationId;
		}

		public void setDraftConversationId(int draftConversationId)
		{
			this.draftConversationId = draftConversationId;
			notifyPropertyChanged(BR.draftConversationId);
		}
	}

	@Override
	public int compareTo(Post post) {
        if (this.getTime() < post.getTime()) return -1;
		if (this.getTime() == post.getTime()) return 0;
		else return 1;
	}
}
