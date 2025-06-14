package com.daquexian.chaoli.forum.view;

import android.app.Activity;
import android.app.ProgressDialog;
import android.databinding.DataBindingUtil;
import android.databinding.Observable;
import android.databinding.ObservableBoolean;
import android.support.v7.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.widget.Toast;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.databinding.ActivityAnswerQuestionsBinding;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.utils.SignUpUtils;

import com.daquexian.chaoli.forum.viewmodel.AnswerQuestionsVM;
import com.daquexian.chaoli.forum.viewmodel.BaseViewModel;

/**
 * 注册时显示问题、回答问题的Activity
 * Created by jianhao on 16-3-28.
 */
public class AnswerQuestionsActivity extends BaseActivity {
    Context mContext;
    Boolean isFirst = true;

    AnswerQuestionsVM viewModel;

    private ProgressDialog mProcessDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        init();

        configToolbar(R.string.answer_quesiton);
    }

    private void init(){
        setViewModel(new AnswerQuestionsVM());
        ActivityAnswerQuestionsBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_answer_questions);
        binding.setViewModel(this.viewModel);

        binding.questionRv.setLayoutManager(new LinearLayoutManager(mContext));
        binding.questionRv.setNestedScrollingEnabled(false);

        final String[] subjectTagsArr = SignUpUtils.subjectTags.keySet().toArray(new String[SignUpUtils.subjectTags.keySet().size()]);
        new AlertDialog.Builder(this).setTitle("请选择科目，综合类测试为6道题，至少需答对4道，分科测试为8道题，至少需答对6道。加油！")
                .setItems(subjectTagsArr, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //SignUpUtils.getQuestionObjList((SignUpUtils.SubmitObserver) mContext, SignUpUtils.subjectTags.get(subjectTagsArr[which]));
                        String subject = SignUpUtils.subjectTags.get(subjectTagsArr[which]);
                        viewModel.getQuestions(subject);
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                })
                .show();

        viewModel.showDialog.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                if (((ObservableBoolean) observable).get()) mProcessDialog = ProgressDialog.show(AnswerQuestionsActivity.this, "", getString(R.string.just_a_sec));
                else mProcessDialog.dismiss();
            }
        });

        viewModel.pass.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                Toast.makeText(getApplicationContext(), "恭喜,答题通过", Toast.LENGTH_SHORT).show();
                ChaoliApplication.getSp().edit().putString(Constants.INVITING_CODE_SP, viewModel.code).apply();
                Bundle bundle = new Bundle();
                bundle.putString("inviteCode", viewModel.code);
                Intent intent = new Intent();
                intent.putExtras(bundle);
                intent.setClass(AnswerQuestionsActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });

        viewModel.fail.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                new AlertDialog.Builder(mContext).setMessage(R.string.you_dont_answer_enough_quesitions_correctly)
                        .setPositiveButton(R.string.try_again, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                isFirst = false;
                                init();
                            }
                        }).setNegativeButton(R.string.dont_try_again, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ((Activity)mContext).finish();
                            }
                        })
                        .setCancelable(false)
                        .show();
            }
        });
    }
    @Override
    public void setViewModel(BaseViewModel viewModel) {
        this.viewModel = (AnswerQuestionsVM) viewModel;
    }
}
