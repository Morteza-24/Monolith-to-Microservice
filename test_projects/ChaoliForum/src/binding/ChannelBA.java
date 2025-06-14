package com.daquexian.chaoli.forum.binding;

import android.databinding.BindingAdapter;

import com.daquexian.chaoli.forum.meta.Channel;
import com.daquexian.chaoli.forum.meta.ChannelTextView;

/**
 * Created by jianhao on 16-9-25.
 */

public class ChannelBA {
    @BindingAdapter("app:channelId")
    public static void setChannel(ChannelTextView channelTextView, int channelId) {
        channelTextView.setChannel(Channel.getChannel(channelId));
    }
}
