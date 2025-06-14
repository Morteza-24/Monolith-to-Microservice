package com.daquexian.chaoli.forum.view;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.databinding.Observable;
import android.databinding.ObservableBoolean;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.widget.EditText;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.databinding.LoginActivityBinding;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.viewmodel.BaseViewModel;
import com.daquexian.chaoli.forum.viewmodel.LoginActivityVM;

public class LoginActivity extends BaseActivity
{
	public static final String TAG = "LoginActivity";

	Context mContext = this;
	LoginActivityVM viewModel;
	ProgressDialog progressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setViewModel(new LoginActivityVM());

		viewModel.clickAQ.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				startActivity(new Intent(LoginActivity.this, AnswerQuestionsActivity.class));
			}
		});

		viewModel.showToast.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				showToast(viewModel.toastContent);
			}
		});

		viewModel.goToMainActivity.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				setResult(RESULT_OK);
				finish();
			}
		});

		viewModel.showProgressDialog.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				if (((ObservableBoolean) observable).get()) {
					progressDialog = ProgressDialog.show(LoginActivity.this, "", getString(R.string.just_a_sec));
					progressDialog.show();
				} else {
					if (progressDialog != null) progressDialog.dismiss();
				}
			}
		});

		viewModel.clickSignUp.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				final EditText inviteCodeET = new EditText(mContext);
				new AlertDialog.Builder(mContext).setTitle(R.string.please_enter_your_invite_code).setView(inviteCodeET)
						.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								Intent intent = new Intent(mContext, SignUpActivity.class);
								intent.putExtra("inviteCode", inviteCodeET.getText().toString());
								mContext.startActivity(intent);
							}
						}).setNegativeButton(android.R.string.cancel, null).show();
			}
		});

		if (ChaoliApplication.getSp().contains(Constants.INVITING_CODE_SP)) {
			showFirstDialog();
		}
	}

	private void showFirstDialog() {
		new AlertDialog.Builder(this)
				.setMessage(R.string.exist_inviting_code_message)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						signUpWithExistingCode();
					}
				})
				.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						showSecondDialog();
					}
				})
				.show();
	}

	private void showSecondDialog() {
		new AlertDialog.Builder(this)
				.setMessage(R.string.exist_inviting_code_message_2)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						signUpWithExistingCode();
					}
				})
				.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						ChaoliApplication.getSp().edit().remove(Constants.INVITING_CODE_SP).apply();
					}
				})
				.show();
	}

	private void signUpWithExistingCode() {
		Intent intent = new Intent(mContext, SignUpActivity.class);
		intent.putExtra("inviteCode", ChaoliApplication.getSp()
				.getString(Constants.INVITING_CODE_SP, ""));
		mContext.startActivity(intent);
	}

	@Override
	public void setViewModel(BaseViewModel viewModel) {
		this.viewModel = (LoginActivityVM) viewModel;
		LoginActivityBinding binding = DataBindingUtil.setContentView(this, R.layout.login_activity);
		binding.setViewModel(this.viewModel);
	}
}
