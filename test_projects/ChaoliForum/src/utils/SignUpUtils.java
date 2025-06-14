package com.daquexian.chaoli.forum.utils;

import android.util.Log;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.model.BusinessQuestion;
import com.daquexian.chaoli.forum.model.Question;
import com.daquexian.chaoli.forum.network.MyOkHttp;
import com.daquexian.chaoli.forum.network.MyRetrofit;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by jianhao on 16-3-27.
 */
public class SignUpUtils {
    private static final String TAG = "SignUpUtils";

    public static final Map<String, String> subjectTags = new HashMap<String, String>(){{
        put("数学", "math");
        put("生物", "bio");
        put("化学", "chem");
        put("物理", "phys");
        put("综合", "");
    }};

    public static int ANSWERS_WRONG = -1;

    public static void getQuestionObjList(final GetQuestionObserver observer, String subject){
        MyRetrofit.getService()
                .getQuestion(subject)
                .enqueue(new Callback<ArrayList<Question>>() {
                    @Override
                    public void onResponse(Call<ArrayList<Question>> call, Response<ArrayList<Question>> response) {
                        observer.onGetQuestionObjList(BusinessQuestion.fromList(response.body()));
                    }

                    @Override
                    public void onFailure(Call<ArrayList<Question>> call, Throwable t) {

                    }
                });
        /*String url = GET_QUESTION_URL + subject;
        client.get(context, url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String response = new String(responseBody);
                ArrayList<Question> questionList = "".equals(response) ? null : (ArrayList<Question>) JSON.parseArray(response, Question.class);
                observer.onGetQuestionObjList(questionList);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.e("error", "link");
                observer.onGetQuestionObjList(null);
            }
        });*/
        //String jsonStr = "{\"_id\":{\"$id\":\"55d7f15669cb38bb2e8b4574\"},\"question\":\"“如无必要，勿增实体”是（）剃刀原理的内容。\",\"choice\":true,\"options\":[\"飞利浦\",\"草薙\",\"无毁之湖光\",\"奥卡姆\"],\"multi_answer\":false}";
        //String jsonStr = "{\"$id\":\"55d7f15669cb38bb2e8b4574\",\"question\":\"“如无必要，勿增实体”是（）剃刀原理的内容。\",\"choice\":true,\"options\":[\"飞利浦\",\"草薙\",\"无毁之湖光\",\"奥卡姆\"],\"multi_answer\":false}";
        //String jsonStr = "{\"id\":\"a\", \"question\":\"b\", \"choice\":\"c\", \"options\":[\"d\"], \"multi_answer\":\"e\" }";
        //String jsonStr = "{\"hi\":\"sdf\"}";
        //ArrayList<Question> questionList = (ArrayList<Question>) JSONArray.parseArray(jsonStr, Question.class);
    }

    public static void submitAnswers(List<BusinessQuestion> questionList, final SubmitObserver observer){
        MyOkHttp.MyOkHttpClient myOkHttpClient = new MyOkHttp.MyOkHttpClient().add("questions", new Gson().toJson(Question.fromList(questionList)));

        //String str = JSON.toJSONString(questionList);
        //RequestParams params = new RequestParams();
        //params.put("questions", str);

        for (BusinessQuestion i: questionList) {
            if (i.choice)
                for (int j = 0; j < i.isChecked.size(); j++) {
                    if (i.isChecked.get(j)) {
                        myOkHttpClient.add(i.id + "_opt[]", String.valueOf(j));
                        Log.d(TAG, "submitAnswers:  id = " + i.id + ", item value = " + j);
                    }
                }
            else
                myOkHttpClient.add(i.id + "_ans", i.answer.get());
        }

        myOkHttpClient.add("simplified", "1");
        myOkHttpClient.post(Constants.CONFIRM_ANSWER_URL)
                .enqueue(ChaoliApplication.getAppContext(), new MyOkHttp.Callback() {
                    @Override
                    public void onFailure(okhttp3.Call call, IOException e) {
                        observer.onFailure(-1);
                    }

                    @Override
                    public void onResponse(okhttp3.Call call, okhttp3.Response response, String responseStr) throws IOException {
                        Log.d(TAG, "onResponse: " + responseStr);
                        if ("failed".equals(responseStr)) {
                            observer.onFailure(ANSWERS_WRONG);
                        } else {
                            observer.onAnswersPass(responseStr);
                        }
                    }
                });

        /*client.post(context, CONFIRM_ANSWER_URL, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String response = new String(responseBody).trim();
                if ("failed".equals(response)) {
                    observer.onFailure(ANSWERS_WRONG);
                } else {
                    observer.onAnswersPass(response);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                observer.onFailure(statusCode);
            }
        });*/
    }

    public interface GetQuestionObserver{
        void onGetQuestionObjList(ArrayList<BusinessQuestion> questionList);
    }
    public interface SubmitObserver {
        void onAnswersPass(String code);
        void onFailure(int statusCode);
    }
}
