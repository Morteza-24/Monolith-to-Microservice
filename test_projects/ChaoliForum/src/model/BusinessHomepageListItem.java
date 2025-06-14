package com.daquexian.chaoli.forum.model;

import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;

import com.daquexian.chaoli.forum.viewmodel.HistoryFragmentVM;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * 对JSON Model的数据进一步处理，专用于显示
 * Created by jianhao on 16-10-3.
 */

public class BusinessHomepageListItem implements Comparable<BusinessHomepageListItem> {
    private final static String TAG = "BusinessHomepageLI";

    public final static String ITEM         = "item";
    public final static String DIVIDER      = "divider";
    public final static String SPACE        = "space";

    public ObservableInt avatarUserId = new ObservableInt();
    public ObservableField<String> avatarUsername = new ObservableField<>();
    public ObservableField<String> avatarSuffix   = new ObservableField<>();
    public ObservableField<String> title          = new ObservableField<>();
    public ObservableField<String> content = new ObservableField<>();
    public ObservableInt postId = new ObservableInt();
    public ObservableInt conversationId = new ObservableInt();
    public ObservableBoolean isFirst = new ObservableBoolean(false);               // 用于决定是否显示分割线（第一个divider不显示分割线） // TODO: 16-10-4
    public String time;
    public String type;

    /**
     * 变换整个List，练习一下RxJava :)
     * @param items List<HistoryFragmentVM.ListItem>
     * @return List<BusinessHomepageListItem>
     */
    public static List<BusinessHomepageListItem> parseList(List<? extends HistoryFragmentVM.ListItem> items) {
        final List<BusinessHomepageListItem> res = new ArrayList<>();
        Observable.from(items)
                .map(new Func1<HistoryFragmentVM.ListItem, BusinessHomepageListItem>() {
                    @Override
                    public BusinessHomepageListItem call(HistoryFragmentVM.ListItem item) {
                        return new BusinessHomepageListItem(item);
                    }
                })
                .subscribe(new Action1<BusinessHomepageListItem>() {
                    @Override
                    public void call(BusinessHomepageListItem businessHomepageListItem) {
                        res.add(businessHomepageListItem);
                    }
                });
        return res;
    }

    public BusinessHomepageListItem(HistoryFragmentVM.ListItem listItem) {
        avatarUserId.set(listItem.getAvatarUserId());
        avatarUsername.set(listItem.getAvatarUsername());
        avatarSuffix.set(listItem.getAvatarSuffix());
        title.set(listItem.getShowingTitle());
        content.set(listItem.getShowingContent());
        postId.set(listItem.getShowingPostId());
        conversationId.set(listItem.getConversationId());
        time = listItem.getTime();
        type = listItem.getType();
    }

    /**
     * 定义全序关系 a > b 表示 ”a排在b后面"，哈哈哈
     * @param o 比较的对象
     * @return 比较的结果
     */
    @Override
    public int compareTo(BusinessHomepageListItem o) {
        if (Long.valueOf(time) < Long.valueOf(o.time)) {
            return 1;
        } else if (Long.valueOf(time).equals(Long.valueOf(o.time))){
            return 0;
        } else {
            return -1;
        }
    }

    public String getType() {
        return type;
    }

    public String getTime() {
        return time;
    }
    /**
     * 将HistoryItem转化为供显示的HomepageListItem
     * @param historyItem 要转化的historyItem
     */
    /*
    public BusinessHomepageListItem(HistoryItem historyItem) {
        switch (historyItem.getType()) {
            case HistoryItem.POST_ACTIVITY:
                break;
            case HistoryItem.STATUS:
                title.set(ChaoliApplication.getAppContext().getString(R.string.modified_his_or_her_information));
                content.set("");
                //holder.description_tv.setText(R.string.modified_his_or_her_information);
                //holder.content_tv.setText("");
                HistoryItem.Data data = historyItem.getData();
                if (data != null && data.getNewStatus() != null) {
                    //holder.content_tv.setText(data.getNewStatus());
                    content.set(data.getNewStatus());
                }
                break;
            case HistoryItem.JOIN:
                title.set(ChaoliApplication.getAppContext().getString(R.string.join_the_forum));
                content.set("");
                //holder.description_tv.setText(R.string.join_the_forum);
                //holder.content_tv.setText("");
                break;
            case DIVIDER:
                //if(position == 1)
                    //holder.divider.setVisibility(View.INVISIBLE);
                // FIXME: 16-10-3
        }
    }

    /**
     * 将NotificationItem转化为HomepageListItem
     * @param notificationItem 要转化的NotificationItem
     */
    /*public BusinessHomepageListItem(NotificationItem notificationItem) {
        switch (notificationItem.getType()) {
            case NotificationItem.DIVIDER:
                //// FIXME: 16-10-3
                //if(position == 1)
                 //   holder.divider.setVisibility(View.INVISIBLE);
                int timeDiff = Integer.parseInt(notificationItem.getTime());
                if(timeDiff == 0){
                    content.set(ChaoliApplication.getAppContext().getString(R.string.today));
                } else {
                    content.set(ChaoliApplication.getAppContext().getString(R.string.days_ago, timeDiff));
                }
                break;
            case NotificationItem.SPACE:

                break;
            default:
                content.set(notificationItem.getData().getTitle());
                postId.set(Integer.parseInt(notificationItem.getData().getPostId()));
                conversationId.set(Integer.parseInt(notificationItem.getData().getConversationId()));
                //holder.content_tv.setText(thisItem.getData().getTitle());
                //holder.description_tv.setText(getString(R.string.mention_you, thisItem.getFromMemberName()));
                //holder.content_tv.setHint(thisItem.getData().getShowingPostId());
                //holder.description_tv.setHint(thisItem.getData().getConversationId());
        }
    }*/
}
