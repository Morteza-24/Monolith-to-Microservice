package com.daquexian.chaoli.forum.model;

import android.databinding.ObservableArrayList;
import android.databinding.ObservableField;
import android.databinding.ObservableList;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jianhao on 16-10-13.
 */

public class BusinessQuestion {
    private static final String TAG = "BusinessQuestion";
    public String id;
    public String question;
    public Boolean choice;
    public Boolean multiAnswer;
    public ObservableList<String> options;

    public ObservableList<Boolean> isChecked = new ObservableArrayList<>();
    public ObservableField<String> answer = new ObservableField<>();

    public BusinessQuestion(Question item) {
        for (int i = 0; i < 4; i++) isChecked.add(false);
        id = item._id.$id;
        question = item.question;
        choice = Boolean.valueOf(item.choice);
        multiAnswer = Boolean.valueOf(item.multi_answer);
        options = new ObservableArrayList<>();
        options.addAll(item.options);
        while (options.size() < 4) {
            options.add(ChaoliApplication.getAppContext().getString(R.string.useless_option));
        }
    }

    public static ArrayList<BusinessQuestion> fromList(List<Question> questionList) {
        ArrayList<BusinessQuestion> businessQuestionList = new ArrayList<>();
        for (Question item : questionList) {
            businessQuestionList.add(new BusinessQuestion(item));
        }
        return businessQuestionList;
    }
}
