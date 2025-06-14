package com.daquexian.chaoli.forum.view;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.databinding.Observable;
import android.databinding.ObservableBoolean;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.databinding.ReplyActionBinding;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.meta.SFXParser3;
import com.daquexian.chaoli.forum.utils.LoginUtils;
import com.daquexian.chaoli.forum.viewmodel.BaseViewModel;
import com.daquexian.chaoli.forum.viewmodel.ReplyActionVM;

import java.util.ArrayList;
import java.util.List;


public class ReplyAction extends BaseActivity
{
	public static final String TAG = "ReplyAction";

	public static final int FLAG_NORMAL = 0;
	public static final int FLAG_REPLY = 1;
	public static final int FLAG_EDIT = 2;

	private static final int MENU_REPLY = 2;
	private static final int MENU_DEMO = 1;

	private List<View> expressionsIVList = new ArrayList<>();

	private ReplyActionVM viewModel;
	private ReplyActionBinding binding;

	private ProgressDialog progressDialog;

	/* 切换至演示模式时保存光标位置，切换回普通模式时恢复 */
	private int selectionStart, selectionEnd;

	private BottomSheetBehavior behavior;
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		int flag;
		int conversationId, postId;
		String replyTo;
		String replyMsg;
		Bundle data = getIntent().getExtras();
		flag = data.getInt("flag");
		conversationId = data.getInt("conversationId");
		postId = data.getInt("postId", -1);
		replyTo = data.getString("replyTo", "");
		replyMsg = data.getString("replyMsg", "");

		//setContentView(R.layout.reply_action);
		setViewModel(new ReplyActionVM(flag, conversationId, postId, replyTo, replyMsg));

		Toolbar toolbar = (Toolbar) findViewById(R.id.tl_custom);
		toolbar.setTitle(R.string.reply);
		toolbar.setTitleTextColor(getResources().getColor(R.color.white));
		setSupportActionBar(toolbar);
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});

		binding.replyText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				Log.d(TAG, "afterTextChanged() called with: editable = [" + editable + "]");
				viewModel.doAfterContentChanged();
			}
		});

		viewModel.replyComplete.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				setResult(RESULT_OK);
				finish();
			}
		});

		binding.replyText.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				tryToShowSoftKeyboard(view);
			}
		});
		viewModel.editComplete.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				finish();
			}
		});

		viewModel.showToast.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				showToast(viewModel.toastContent.get());
			}
		});

		viewModel.updateRichText.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
			}
		});

		viewModel.demoMode.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				if (((ObservableBoolean) observable).get()) {
					selectionStart = binding.replyText.getSelectionStart();
					selectionEnd = binding.replyText.getSelectionEnd();
					binding.replyText.setEnabled(false);
					binding.replyText.setOnlineImgEnabled(true);
					binding.replyText.update();
				} else {
					binding.replyText.setEnabled(true);
					binding.replyText.setOnlineImgEnabled(false);
					binding.replyText.update();
					binding.replyText.setSelection(selectionStart, selectionEnd);
				}
			}
		});

		viewModel.showWelcome.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				new AlertDialog.Builder(ReplyAction.this).setMessage(R.string.welcome_to_demo_mode)
						.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int i) {

							}
						})
						.show();
			}
		});

		viewModel.showDialog.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				if (((ObservableBoolean) observable).get()) progressDialog = ProgressDialog.show(ReplyAction.this, "", getString(R.string.just_a_sec));
				else progressDialog.dismiss();
			}
		});

		/**
		 * 让各个表情按钮响应单击事件
		 */

		View.OnClickListener onClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (behavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
					for (int j = 0; j < Constants.icons.length; j++) {
						String icon = Constants.icons[j];
						CharSequence contentDescription =  view.getContentDescription();
						if (icon.equals(contentDescription)) {
							int start = Math.max(binding.replyText.getSelectionStart(), 0);
							int end = Math.max(binding.replyText.getSelectionEnd(), 0);
							binding.replyText.getText().replace(Math.min(start, end), Math.max(start, end),
									Constants.iconStrs[j], 0, Constants.iconStrs[j].length());
							updateRichText();
							break;
						}
					}
				}
			}
		};

		final ViewGroup expressions = (ViewGroup) ((ViewGroup) binding.expressions).getChildAt(0);
		for (int i = 0; i < expressions.getChildCount(); i++) {
			ViewGroup subView = (ViewGroup) expressions.getChildAt(i);
			for (int j = 0; j < subView.getChildCount(); j++) {
				View expressionView = subView.getChildAt(j);
				expressionsIVList.add(expressionView);
				expressionView.setAlpha(Constants.MIN_EXPRESSION_ALPHA);
				expressionView.setOnClickListener(onClickListener);
			}
		}

		behavior = BottomSheetBehavior.from(binding.bottomSheet);
		behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
			@Override
			public void onStateChanged(@NonNull View bottomSheet, int newState) {
				//bottomSheetState = newState;
			}

			@Override
			public void onSlide(@NonNull View bottomSheet, float slideOffset) {
				for (View expressionView : expressionsIVList) {
					expressionView.setAlpha(Constants.MIN_EXPRESSION_ALPHA + slideOffset * (1 - Constants.MIN_EXPRESSION_ALPHA));
				}
			}
		});

        binding.replyText.requestFocus();
        tryToShowSoftKeyboard(binding.replyText);
	}

	private void updateRichText() {
		int selectionStart = binding.replyText.getSelectionStart();
		int selectionEnd = binding.replyText.getSelectionEnd();
		binding.replyText.setText(SFXParser3.parse(getApplicationContext(), viewModel.content.get(), null));
		binding.replyText.setSelection(selectionStart, selectionEnd);
	}

	private void tryToShowSoftKeyboard(View view) {
		if (behavior.getState() == BottomSheetBehavior.STATE_COLLAPSED || behavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showSoftInput(view, InputMethodManager.SHOW_FORCED);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		menu.add(Menu.NONE, Menu.NONE, MENU_DEMO, R.string.post).setIcon(R.drawable.ic_functions_white_24dp).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		menu.add(Menu.NONE, Menu.NONE, MENU_REPLY, R.string.reply).setIcon(R.drawable.ic_cab_done_mtrl_alpha).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		super.onOptionsItemSelected(item);
		switch (item.getOrder())
		{
			case MENU_REPLY:
				switch (viewModel.flag.get())
				{
					case FLAG_NORMAL:
						if (!LoginUtils.isLoggedIn()){
							showToast(R.string.please_login);
							break;
						}
						viewModel.reply();
						break;
					case FLAG_EDIT:
						viewModel.edit();
						break;
				}
				break;
			case MENU_DEMO:
				viewModel.changeDemoMode();
				break;
		}
		return true;
	}

	@Override
	public void onBackPressed() {
		if (behavior.getState() == BottomSheetBehavior.STATE_EXPANDED) behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
		else super.onBackPressed();
	}

	@Override
	protected void onPause() {
		super.onPause();
		View view = this.getCurrentFocus();
		if (view != null) {
			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
	}

	@Override
	public void setViewModel(BaseViewModel viewModel) {
		this.viewModel = (ReplyActionVM) viewModel;
		binding = DataBindingUtil.setContentView(this, R.layout.reply_action);
		binding.setViewModel(this.viewModel);
	}
}
