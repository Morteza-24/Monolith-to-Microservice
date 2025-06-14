package com.daquexian.chaoli.forum.meta;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.TextView;

public class ChannelTextView extends TextView
{
	private Channel channel;

	private static final String TAG = "ChannelTextView";

	public ChannelTextView(Context context, Channel channel)
	{
		this(context);
		this.channel = channel;
		this.setPadding(5, 5, 5, 5);
		this.setText(channel.toString());
		this.setTextColor(channel.getColor());
	}

	public ChannelTextView(Context context)
	{
		this(context, (AttributeSet) null);
	}

	public ChannelTextView(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);
	}

	public ChannelTextView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);
		Paint paint = new Paint();
		paint.setColor(this.getChannel().getColor());
		canvas.drawLine(0, 0, this.getWidth() - 1, 0, paint);
		canvas.drawLine(0, 0, 0, this.getHeight() - 1, paint);
		canvas.drawLine(this.getWidth() - 1, 0, this.getWidth() - 1, this.getHeight() - 1, paint);
		canvas.drawLine(0, this.getHeight() - 1, this.getWidth() - 1, this.getHeight() - 1, paint);
	}

	public Channel getChannel()
	{
		if (channel == null) return Channel.caff;
		return channel;
	}

	public void setChannel(Channel channel)
	{
		this.channel = channel;
		this.setPadding(5, 5, 5, 5);
		this.setText(channel.toString());
		this.setTextColor(channel.getColor());
		//invalidate();
	}
}
