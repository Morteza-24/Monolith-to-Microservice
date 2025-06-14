package com.daquexian.chaoli.forum.utils;

import android.content.Context;
import android.util.Log;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.data.Me;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.network.MyOkHttp;
import com.daquexian.chaoli.forum.network.MyOkHttp.Callback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Response;

/**
 * Created by jianhao on 16-3-11.
 */
public class ConversationUtils {
    private static final String TAG = "ConversationUtils";
    /* 茶馆 */
    public static final int CAFF_ID = 1;
    /* 数学 */
    public static final int MATH_ID = 4;
    /* 物理 */
    public static final int PHYS_ID = 5;
    /* 化学 */
    public static final int CHEM_ID = 6;
    /* 生物 */
    public static final int BIO_ID = 7;
    /* 技术 */
    public static final int TECH_ID = 8;
    /* 公告 */
    public static final int ANNOUN_ID = 28;
    /* 申诉 */
    public static final int COURT_ID = 25;
    /* 回收站 */
    public static final int RECYCLED_ID = 27;
    /* 语言 */
    public static final int LANG_ID = 40;
    /* 社科 */
    public static final int SOCSCI_ID = 34;

    /* 返回的数据错误 */
    public static final int RETURN_ERROR = -1;
    /* 取消可见用户时没有可见用户列表没有这个用户 */
    public static final int NO_THIS_MEMBER = -2;

    private static ArrayList<Integer> memberList = new ArrayList<>();

    public static void setChannel(int channel, SetChannelObserver Observer){
        setChannel(channel, 0, Observer);
    }

    /**
     * 设置主题的板块
     * conversationId为0时，会设置正在编辑、还未发出的conversation的板块
     * addMember, removeMember也是同样
     * @param channel 板块号
     * @param conversationId 主题id
     * @param observer observer
     */
    public static void setChannel(int channel, int conversationId, final SetChannelObserver observer){
        String url = Constants.SET_CHANNEL_URL + String.valueOf(conversationId);
        new MyOkHttp.MyOkHttpClient()
                .add("channel", String.valueOf(channel))
                .add("userId", String.valueOf(Me.getUserId()))
                .add("token", LoginUtils.getToken())
                .post(url)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        observer.onSetChannelFailure(-3);
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        if (response.code() != 200) observer.onSetChannelFailure(response.code());
                        else {
                            if (responseStr.startsWith("{\"allowedSummary\"")) {
                                observer.onSetChannelSuccess();
                            } else {
                                Log.d(TAG, "onResponse: " + responseStr);
                                observer.onSetChannelFailure(RETURN_ERROR);                 //返回数据错误
                            }
                        }
                    }
                });
    }

    /*  添加可见用户（默认任何人均可见）    */
    public static void addMember(String member, AddMemberObserver Observer){
        addMember(member, 0, Observer);
    }

    public static void addMember(String member, int conversationId, final AddMemberObserver observer){
        String url = Constants.ADD_MEMBER_URL + String.valueOf(conversationId);
        new MyOkHttp.MyOkHttpClient()
                .add("member", member)
                .add("userId", String.valueOf(Me.getUserId()))
                .add("token", LoginUtils.getToken())
                .post(url)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        observer.onAddMemberFailure(-3);
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        if (response.code() != 200) observer.onAddMemberFailure(response.code());
                        else {
                            //Log.i("addMember", response);
                            String idFormat = "data-id='(\\d+)'";
                            Pattern pattern = Pattern.compile(idFormat);
                            Matcher matcher = pattern.matcher(responseStr);
                            //Log.i("response", String.valueOf(response.length()));
                            if (matcher.find()) {
                                //Log.i("id", matcher.group(1));
                                int userIdAdded = Integer.parseInt(matcher.group(1));
                                memberList.add(userIdAdded);
                                observer.onAddMemberSuccess();
                            } else {
                                observer.onAddMemberFailure(RETURN_ERROR);              //返回数据错误
                            }
                        }
                    }
                });
    }

    /*  取消可见用户  */
    public static void removeMember(final Context context, int userId, RemoveMemberObserver Observer){
        removeMember(context, userId, 0, Observer);
    }

    public static void removeMember(final Context context, final int userId,
                                    int conversationId, final RemoveMemberObserver observer){
        String url = Constants.REMOVE_MEMBER_URL + String.valueOf(conversationId);
        new MyOkHttp.MyOkHttpClient()
                .add("member", String.valueOf(userId))
                .add("userId", String.valueOf(Me.getUserId()))
                .add("token", LoginUtils.getToken())
                .post(url)
                .enqueue(context, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        observer.onRemoveMemberFailure(-3);
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        if (response.code() != 200) observer.onRemoveMemberFailure(response.code());
                        else {
                            Log.i("removeMember", responseStr);
                            if (responseStr.startsWith("{\"allowedSummary\"")) {
                                if (memberList.contains(Integer.valueOf(userId))) {
                                    memberList.remove(Integer.valueOf(userId));
                                    observer.onRemoveMemberSuccess();
                                } else {
                                    observer.onRemoveMemberFailure(NO_THIS_MEMBER);         //无此会员
                                }
                            } else {
                                observer.onRemoveMemberFailure(RETURN_ERROR);
                            }
                        }
                    }
                });
    }

    /*  发表主题    */
    public static void postConversation(String title, String content,
                                        final PostConversationObserver observer){
        new MyOkHttp.MyOkHttpClient()
                .add("title", title)
                .add("content", content)
                .add("userId", String.valueOf(Me.getUserId()))
                .add("token", LoginUtils.getToken())
                .post(Constants.POST_CONVERSATION_URL)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        observer.onPostConversationFailure(-3);
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        if (response.code() != 200) observer.onPostConversationFailure(response.code());
                        else {
                            Log.d(TAG, "onResponse: " + responseStr);
                            if(responseStr.startsWith("{\"redirect\"")){
                                String conIdFormat = "/(\\d+)";
                                Pattern pattern = Pattern.compile(conIdFormat);
                                Matcher matcher = pattern.matcher(responseStr);
                                if(matcher.find()){
                                    observer.onPostConversationSuccess(Integer.parseInt(matcher.group(1)));
                                }else{
                                    observer.onPostConversationFailure(RETURN_ERROR);
                                }
                            } else {
                                observer.onPostConversationFailure(RETURN_ERROR);
                            }
                        }
                    }
                });
    }

    /*  获取可见用户列表    */
    public static void getMembersAllowed(Context context, int conId, final GetMembersAllowedObserver observer){
        final List<Integer> memberList = new ArrayList<>();
        String url = Constants.GET_MEMBERS_ALLOWED_URL + "?p=conversation/membersAllowed.ajax/" + conId;
        new MyOkHttp.MyOkHttpClient()
                .add("token", LoginUtils.getToken())
                .add("userId", String.valueOf(Me.getUserId()))
                .get(url)
                .enqueue(context, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        observer.onGetMembersAllowedFailure(-3);
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        if (response.code() != 200) observer.onGetMembersAllowedFailure(response.code());
                        else {
                            Log.i("get", responseStr);
                            String idFormat = "data-id='(\\d+)'";           //按此格式从返回的数据中获取id
                            Pattern pattern = Pattern.compile(idFormat);
                            Matcher matcher = pattern.matcher(responseStr);
                            while (matcher.find()) {
                                memberList.add(Integer.valueOf(matcher.group(1)));
                            }

                            //返回的数据中，若可见用户只有自己，则返回自己的id，若有其他人，则不包含自己的id，所以要加上自己的id
                            if (memberList.size() > 1 || (memberList.size() == 1 && memberList.get(0) != Me.getUserId())) {
                                memberList.add(Me.getUserId());
                            }
                            observer.onGetMembersAllowedSuccess(memberList);
                        }

                    }
                });
    }

    /*  隐藏/取消隐藏该主题
    *   执行操作后主题的状态为隐藏，则isIgnored为true，否则为false*/
    public static void ignoreConversation(Context context, int conversationId,
                                          final IgnoreAndStarConversationObserver observer){
        String url = Constants.IGNORE_CONVERSATION_URL + conversationId;
        new MyOkHttp.MyOkHttpClient()
                .add("userId", String.valueOf(Me.getUserId()))
                .add("token", LoginUtils.getToken())
                .get(url)
                .enqueue(context, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        observer.onIgnoreConversationFailure(-3);
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        if (response.code() != 200) observer.onIgnoreConversationFailure(response.code());
                        else {
                            if(responseStr.contains("\"ignored\":true")) {
                                observer.onIgnoreConversationSuccess(true);
                            }else if(responseStr.contains("\"ignored\":false")){
                                observer.onIgnoreConversationSuccess(false);
                            }else{
                                Log.e("ignore", "response = " + responseStr);
                                observer.onIgnoreConversationFailure(RETURN_ERROR);
                            }
                        }
                    }
                });
    }

    /*  关注/取消关注该主题
    *   执行操作后主题的状态为被关注，则isStarred为true，否则为false*/
    public static void starConversation(Context context, int conversationId,
                                        final IgnoreAndStarConversationObserver observer) {
        String url = Constants.STAR_CONVERSATION_URL + conversationId;
        new MyOkHttp.MyOkHttpClient()
                .add("userId", String.valueOf(Me.getUserId()))
                .add("token", LoginUtils.getToken())
                .get(url)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        observer.onStarConversationFailure(-3);
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        if (response.code() != 200) observer.onStarConversationFailure(response.code());
                        else {
                            if (responseStr.contains("\"starred\":true")) {
                                observer.onStarConversationSuccess(true);
                            } else if (responseStr.contains("\"starred\":false")) {
                                observer.onStarConversationSuccess(false);
                            } else {
                                observer.onStarConversationFailure(RETURN_ERROR);
                            }
                        }
                    }
                });
    }

    public static Boolean canDelete(int conversationId) {
        return false;
    }

    public static Boolean canEdit(int conversationId) {
        return false;
    }

    public interface SetChannelObserver {
        void onSetChannelSuccess();
        void onSetChannelFailure(int statusCode);
    }

    public interface AddMemberObserver {
        void onAddMemberSuccess();
        void onAddMemberFailure(int statusCode);
    }

    public interface RemoveMemberObserver {
        void onRemoveMemberSuccess();
        void onRemoveMemberFailure(int statusCode);
    }

    public interface PostConversationObserver {
        void onPostConversationSuccess(int conversationId);
        void onPostConversationFailure(int statusCode);
    }

    public interface GetMembersAllowedObserver {
        void onGetMembersAllowedSuccess(List<Integer> memberList);
        void onGetMembersAllowedFailure(int statusCode);
    }

    public interface IgnoreAndStarConversationObserver{
        void onIgnoreConversationSuccess(Boolean isIgnored);
        void onIgnoreConversationFailure(int statusCode);
        void onStarConversationSuccess(Boolean isStarred);
        void onStarConversationFailure(int statusCode);
    }
}
