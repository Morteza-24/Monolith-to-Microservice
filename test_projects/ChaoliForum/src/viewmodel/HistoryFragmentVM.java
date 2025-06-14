package com.daquexian.chaoli.forum.viewmodel;

import android.databinding.ObservableArrayList;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.databinding.ObservableList;
import android.util.Log;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.binding.HistoryLayoutSelector;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.model.BusinessHomepageListItem;
import com.daquexian.chaoli.forum.model.HistoryItem;
import com.daquexian.chaoli.forum.model.HistoryResult;
import com.daquexian.chaoli.forum.model.NotificationResult;
import com.daquexian.chaoli.forum.network.MyOkHttp;
import com.daquexian.chaoli.forum.utils.MyUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Response;

/**
 * ViewModel for HistoryFragment
 * Created by jianhao on 16-10-2.
 */

public class HistoryFragmentVM extends BaseViewModel {
    private static String TAG = "HistoryFVM";

    public static final int TYPE_ACTIVITY = 0;
    public static final int TYPE_NOTIFICATION = 1;

    private int type = TYPE_ACTIVITY;   //type == 0表示History, type == 1表示Notification

    public ObservableList<BusinessHomepageListItem> showingItemList = new ObservableArrayList<>();

    private boolean canLoad = true;

    public ObservableBoolean isRefreshing = new ObservableBoolean();
    public ObservableBoolean showProgressDialog = new ObservableBoolean(false);
    public ObservableInt intendedConversationId = new ObservableInt();
    public ObservableField<String> intendedConversationTitle = new ObservableField<>();
    public ObservableInt intendedConversationPage = new ObservableInt();
    public ObservableInt goToPost = new ObservableInt();
    public ObservableBoolean showToast = new ObservableBoolean();
    public String toastContent;

    private int userId;
    private String username;
    private String avatarSuffix;

    public String url = Constants.GET_ACTIVITIES_URL;

    public int page = 1;

    public HistoryFragmentVM(int type, int userId, String username, String avatarSuffix) {
        this.type = type;
        this.userId = userId;
        this.username = username;
        this.avatarSuffix = avatarSuffix;
        url = type == TYPE_ACTIVITY ? Constants.GET_ACTIVITIES_URL + userId : Constants.GET_ALL_NOTIFICATIONS_URL;
    }

    public HistoryLayoutSelector layoutSelector = new HistoryLayoutSelector();

    public void clickItem(BusinessHomepageListItem item) {
        //final ProgressDialog progressDialog = ProgressDialog.show(context, "", getResources().getString(R.string.just_a_sec));
        Log.d(TAG, "clickItem() called with: item = [" + item + "]");
        showProgressDialog.set(true);
        new MyOkHttp.MyOkHttpClient()
                .get(Constants.GO_TO_POST_URL + item.postId.get())
                .enqueue(new MyOkHttp.Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        showProgressDialog.set(false);
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        //Intent intent = new Intent(mCallback, PostActivity.class);

                        Pattern pattern = Pattern.compile("\"conversationId\":(\\d+)");
                        Matcher matcher = pattern.matcher(responseStr);
                        if (matcher.find()) {
                            intendedConversationId.set(Integer.parseInt(matcher.group(1)));
                        } else {
                            toastContent = getString(R.string.conversation_has_been_deleted);
                            showToast.notifyChange();
                            return;
                        }

                        pattern = Pattern.compile("<h1 id='conversationTitle'>(.*?)</h1>");
                        matcher = pattern.matcher(responseStr);
                        if (matcher.find()) {
                            String title = matcher.group(1);
                            title = title.replaceAll("(^<(.*?)>)|(<(.*?)>$)", "");
                            //intent.putExtra("title", title);
                            intendedConversationTitle.set(title);
                        } else {
                            toastContent = getString(R.string.conversation_has_been_deleted);
                            showToast.notifyChange();
                            return;
                        }

                        //if (v.equals(holder.content_tv)) {
                            pattern = Pattern.compile("\"startFrom\":(\\d+)");
                            matcher = pattern.matcher(responseStr);
                            if (matcher.find()) {
                                int intentToPage = Integer.parseInt(matcher.group(1)) / 20 + 1;
                                intendedConversationPage.set(intentToPage);
                            } else {
                                intendedConversationPage.set(-1);
                            }
                        //}
                        showProgressDialog.set(false);
                        goToPost.notifyChange();
                    }
                });
    }

    public void refresh() {
        isRefreshing.set(true);
        new MyOkHttp.MyOkHttpClient()
                .get(url)
                .enqueue(new MyOkHttp.Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        //swipyRefreshLayout.setRefreshing(false);
                        isRefreshing.set(false);
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        List<BusinessHomepageListItem> listItems = parseItems(responseStr);

                        //MyUtils.expandUnique(showingItemList, listItems, false);
                        //Log.d(TAG, String.valueOf(myAdapter.listItems.size()));
                        showingItemList.clear();
                        showingItemList.addAll(listItems);
                        addTimeDivider(showingItemList);

                        //myAdapter.notifyDataSetChanged();
                        //swipyRefreshLayout.setRefreshing(false);
                        isRefreshing.set(false);
                    }
                });
    }

    public void tryToLoadMore() {
        if (canLoad) {
            canLoad = false;
            loadMore();
        }
    }

    public void loadMore() {
        isRefreshing.set(true);
        Log.d(TAG, "loadMore: " + page);
        new MyOkHttp.MyOkHttpClient()
                .get(url + "/" + (page + 1))
                .enqueue(new MyOkHttp.Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        isRefreshing.set(false);
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        page++;
                        List<BusinessHomepageListItem> listItems = parseItems(responseStr);

                        final int listSize = listItems.size();
                        if(listSize == 0){
                            isRefreshing.set(false);
                            return;
                        }

                        int oldSize = showingItemList.size();

                        MyUtils.expandUnique(showingItemList, listItems);

                        canLoad = oldSize != showingItemList.size();

                        addTimeDivider(showingItemList);
                        isRefreshing.set(false);
                    }
                });
    }

    private List<BusinessHomepageListItem> parseItems(String responseStr) {
        if (type == TYPE_ACTIVITY) {
            Gson gson = new GsonBuilder().registerTypeAdapterFactory(new HistoryAdapterFactory()).create();
            HistoryResult historyResult = gson.fromJson(responseStr, HistoryResult.class);
            return BusinessHomepageListItem.parseList(historyResult.activity);
        } else {
            NotificationResult notificationResult = new Gson().fromJson(responseStr, NotificationResult.class);
            return BusinessHomepageListItem.parseList(notificationResult.getResults());
        }
    }

    /**
     * Data不存在时为false，存在时为一个json对象，必须在它为false的时候跳过，否则会产生错误
     */
    private static class HistoryAdapterFactory implements TypeAdapterFactory {
        @Override
        @SuppressWarnings("unchecked")
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (type.getRawType() != HistoryItem.Data.class) return null;

            TypeAdapter<HistoryItem.Data> defaultAdapter = (TypeAdapter<HistoryItem.Data>) gson.getDelegateAdapter(this, type);
            return (TypeAdapter<T>) new DataAdapter(defaultAdapter);

        }

        public class DataAdapter extends TypeAdapter<HistoryItem.Data> {
            TypeAdapter<HistoryItem.Data> defaultAdapter;

            DataAdapter(TypeAdapter<HistoryItem.Data> defaultAdapter) {
                this.defaultAdapter = defaultAdapter;
            }
            @Override
            public void write(JsonWriter out, HistoryItem.Data value) throws IOException {
                defaultAdapter.write(out, value);
            }

            @Override
            public HistoryItem.Data read(JsonReader in) throws IOException {
                if (in.peek() == JsonToken.BOOLEAN) {
                    in.skipValue();
                    return null;
                }
                return defaultAdapter.read(in);
            }
        }
    }


    public void addTimeDivider(List<BusinessHomepageListItem> items){
        int dateNow;
        if(items.size() > 0 && !BusinessHomepageListItem.DIVIDER.equals(items.get(0).getType()) && !BusinessHomepageListItem.SPACE.equals(items.get(0).getType())){
            int firstDate = (int) ((Long.parseLong(items.get(0).getTime()) + 8 * 60 * 60) / 24 / 60 / 60);
            dateNow = (int) (Calendar.getInstance().getTimeInMillis() / 1000 / 24 / 60 / 60);
            items.add(0, new BusinessHomepageListItem(new Divider(dateNow - firstDate)));
        }
        //if(items.size() > 0 && !(items.get(0).getType().equals(BusinessHomepageListItem.SPACE))) items.add(0, new BusinessHomepageListItem(new Space()));
        for(int i = 0; i < items.size() - 1; i++){
            BusinessHomepageListItem thisItem = items.get(i), nextItem = items.get(i + 1);
            if(!BusinessHomepageListItem.DIVIDER.equals(thisItem.getType()) && !BusinessHomepageListItem.DIVIDER.equals(nextItem.getType())
                    && !BusinessHomepageListItem.SPACE.equals(thisItem.getType()) && !BusinessHomepageListItem.SPACE.equals(nextItem.getType())){
                int thisDate = (int) ((Long.parseLong(thisItem.getTime()) + 8 * 60 * 60) / 24 / 60 / 60);
                int nextDate = (int) ((Long.parseLong(nextItem.getTime()) + 8 * 60 * 60) / 24 / 60 / 60);
                if(thisDate != nextDate) {
                    dateNow = (int) (Calendar.getInstance().getTimeInMillis() / 1000 / 24 / 60 / 60);
                    items.add(i + 1, new BusinessHomepageListItem(new Divider(dateNow - nextDate)));
                }
            }
        }
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public abstract static class ListItem implements Comparable<ListItem> {
        public final static String ITEM         = "item";
        public final static String DIVIDER      = "divider";
        public final static String SPACE        = "space";

        public String getType() {
            return type;
        }

        public String getTime() {
            return time;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public abstract String getShowingTitle();
        public abstract String getShowingContent();
        public abstract int getShowingPostId();
        public abstract int getConversationId();
        public abstract String getAvatarSuffix();
        public abstract String getAvatarUsername();
        public abstract int getAvatarUserId();

        String type, time;

        @Override
        public int compareTo(ListItem B) {
            if (Long.valueOf(time) > Long.valueOf(B.time)) {
                return 1;
            } else if (Long.valueOf(time) == Long.valueOf(B.time)){
                return 0;
            } else {
                return -1;
            }
        }
    }

    static class Divider extends ListItem{
        String time;
        @Override
        public String getType() {
            return ListItem.DIVIDER;
        }

        @Override
        public String getTime() {
            return time;
        }

        Divider(int time){
            this.time = String.valueOf(time);
        }

        @Override
        public String getShowingTitle() {
            return null;
        }

        @Override
        public String getShowingContent() {
            int timeDiff = Integer.parseInt(getTime());
            if(timeDiff == 0){
                //content.set(ChaoliApplication.getAppContext().getString(R.string.today));
                return ChaoliApplication.getAppContext().getString(R.string.today);
            } else {
                //holder.time_tv.setText(getString(R.string.days_ago, timeDiff));
                //content.set(ChaoliApplication.getAppContext().getString(R.string.days_ago, timeDiff));
                return ChaoliApplication.getAppContext().getString(R.string.days_ago, timeDiff);
            }
        }

        @Override
        public int getAvatarUserId() {
            return 0;
        }

        @Override
        public int getConversationId() {
            return 0;
        }

        @Override
        public int getShowingPostId() {
            return 0;
        }

        @Override
        public String getAvatarSuffix() {
            return null;
        }

        @Override
        public String getAvatarUsername() {
            return null;
        }
    }

    static class Space extends ListItem{
        @Override
        public String getTime() {
            return null;
        }

        @Override
        public String getType() {
            return ListItem.SPACE;
        }

        @Override
        public int getShowingPostId() {
            return 0;
        }

        @Override
        public int getConversationId() {
            return 0;
        }

        @Override
        public String getShowingContent() {
            return null;
        }

        @Override
        public String getAvatarUsername() {
            return null;
        }

        @Override
        public String getAvatarSuffix() {
            return null;
        }

        @Override
        public int getAvatarUserId() {
            return 0;
        }

        @Override
        public String getShowingTitle() {
            return null;
        }
    }
}
