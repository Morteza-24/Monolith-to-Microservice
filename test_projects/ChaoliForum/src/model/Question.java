package com.daquexian.chaoli.forum.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jianhao on 16-9-3.
 * 注册时的问题
 */
public class Question {
    public class id{
        public String $id;
    }
    public id _id;

    public String getQuestion() {
        return question;
    }

    public Boolean isMultiAnswer(){
        return Boolean.valueOf(multi_answer);
    }

    public List<String> getOptions() {
        return options;
    }

    public String getChoice() {
        return choice;
    }

    public String question, choice, multi_answer;

    public List<String> options = new ArrayList<>();
    public List<String> answers = new ArrayList<>();

    public Question() {}
    public Question(BusinessQuestion businessQuestion) {
        _id = new id();
        _id.$id = businessQuestion.id;
        choice = String.valueOf(businessQuestion.choice);
        multi_answer = String.valueOf(businessQuestion.multiAnswer);
        if (choice.equals("false")) answers.add(businessQuestion.answer.get());
        else {
            for (int i = 0; i < businessQuestion.isChecked.size(); i++) {
                if (businessQuestion.isChecked.get(i)) answers.add(String.valueOf(i));
            }
        }
    }

    public static ArrayList<Question> fromList(List<BusinessQuestion> list) {
        ArrayList<Question> questionList = new ArrayList<>();
        for (BusinessQuestion businessQuestion : list) {
            questionList.add(new Question(businessQuestion));
        }
        return questionList;
    }
}
