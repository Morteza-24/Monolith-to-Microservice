package com.daquexian.chaoli.forum.meta;

public enum ConversationState
{
	normal("普通"),
	sticky("版内置顶"),
	starred("关注"),
	featured("精品"),
	draft("草稿"),
	ignored("隐藏"),
	question("未解决"),
	answered("已解决");

	private String detail;

	ConversationState(String detail)
	{
		this.detail = detail;
	}

	public String getDetail()
	{
		return detail;
	}

	@Override
	public String toString()
	{
		return this.detail;
	}
}
