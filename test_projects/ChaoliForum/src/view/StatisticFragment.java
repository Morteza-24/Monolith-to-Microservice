package com.daquexian.chaoli.forum.view;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.network.MyOkHttp;
import com.daquexian.chaoli.forum.network.MyOkHttp.Callback;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Response;


/**
 * 在个人主页显示用户数据的Fragment
 * Created by jianhao on 16-6-5.
 */
public class StatisticFragment extends Fragment {
    Context mCallback;
    int mUserId;

    private TextView postTxt;
    private TextView conversationTxt;
    private TextView joinedConversationTxt;
    private TextView earliestPostTxt;
    private TextView joinBBSTxt;
    private TextView jinpinTxt;

    private final String TAG = "StatisticsFragment";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserId = getArguments().getInt("userId");
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallback = context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.homepage_statistics, container, false);
        final int[] intStats = new int[3];
        final String[] strStats = new String[3];

        postTxt = (TextView) view.findViewById(R.id.postTxt);
        conversationTxt = (TextView) view.findViewById(R.id.conversationTxt);
        joinedConversationTxt = (TextView) view.findViewById(R.id.joinedConversationTxt);
        earliestPostTxt = (TextView) view.findViewById(R.id.earliestPostTxt);
        joinBBSTxt = (TextView) view.findViewById(R.id.joinBBSTxt);
        jinpinTxt = (TextView) view.findViewById(R.id.jinpinTxt);

        new MyOkHttp.MyOkHttpClient()
                .get(Constants.GET_STATISTICS_URL + mUserId)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Toast.makeText(mCallback, R.string.network_err, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        response.body().close();
                        Pattern pattern = Pattern.compile("<div>(\\\\n)?(.*?)<");
                        Matcher matcher = pattern.matcher(responseStr);
                        if(matcher.find()){
                            intStats[0] = Integer.parseInt(matcher.group(2));
                            postTxt.setText(String.valueOf(intStats[0]));
                        }
                        if(matcher.find()){
                            intStats[1] = Integer.parseInt(matcher.group(2));
                            conversationTxt.setText(String.valueOf(intStats[1]));
                        }
                        if (matcher.find()) {
                            intStats[2] = Integer.parseInt(matcher.group(2));
                            joinedConversationTxt.setText(String.valueOf(intStats[2]));
                        }
                        if (matcher.find()) {
                            strStats[0] = unicodeToString(matcher.group(2));
                            earliestPostTxt.setText(strStats[0]);
                        }
                        if (matcher.find()) {
                            strStats[1] = unicodeToString(matcher.group(2));
                            joinBBSTxt.setText(strStats[1]);
                        }
                        if (matcher.find()) {
                            strStats[2] = unicodeToString(matcher.group(2));
                            jinpinTxt.setText(strStats[2]);
                        }
                    }
                });
        return view;
    }


    public static String unicodeToString(String str) {
        Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
        Matcher matcher = pattern.matcher(str);
        char ch;
        while (matcher.find()) {
            ch = (char) Integer.parseInt(matcher.group(2), 16);
            str = str.replace(matcher.group(1), ch + "");
        }
        return str;
    }
}
