package com.daquexian.chaoli.forum.view;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.databinding.Observable;
import android.databinding.ObservableBoolean;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.databinding.ActivitySignUpBinding;
import com.daquexian.chaoli.forum.viewmodel.BaseViewModel;
import com.daquexian.chaoli.forum.viewmodel.SignUpVM;

/**
 * Created by jianhao on 16-4-7.
 * SignUpActivity
 */

public class SignUpActivity extends BaseActivity {
    final static String TAG = "SignUpActivity";

    Context mContext;
    String mToken;

    ProgressDialog progressDialog;

    SignUpVM viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_sign_up);
        mContext = this;

        Bundle bundle = getIntent().getExtras();
        String inviteCode = bundle == null ? "" :bundle.getString("inviteCode", "");

        if("".equals(inviteCode)){
            Toast.makeText(getApplicationContext(), R.string.you_can_only_sign_up_with_an_invite_code, Toast.LENGTH_SHORT).show();
            finish();
        }

        setViewModel(new SignUpVM(inviteCode));
        viewModel.init();
        configToolbar(R.string.sign_up);

        viewModel.showToast.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                showToast(viewModel.toastContent.get());
            }
        });

        viewModel.showProcessDialog.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                if (((ObservableBoolean) observable).get()) {
                    progressDialog = ProgressDialog.show(mContext, "", getString(R.string.just_a_sec));
                } else {
                    if (progressDialog != null) progressDialog.dismiss();
                }
            }
        });

        viewModel.signUpSuccess.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                signUpSuccess();
            }
        });
    }

    private void signUpSuccess() {
        Toast.makeText(getApplicationContext(), R.string.sign_up_successfully, Toast.LENGTH_LONG).show();
        Intent intent = new Intent();
        intent.setClass(SignUpActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);   //清除所在栈所有Activity
        startActivity(intent);
    }

    @Override
    public void setViewModel(BaseViewModel viewModel) {
        this.viewModel = (SignUpVM) viewModel;
        ActivitySignUpBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_sign_up);
        binding.setViewModel(this.viewModel);
    }
}
