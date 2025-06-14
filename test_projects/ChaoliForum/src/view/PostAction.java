package com.daquexian.chaoli.forum.view;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.databinding.Observable;
import android.databinding.ObservableBoolean;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.databinding.PostActionBinding;
import com.daquexian.chaoli.forum.meta.Channel;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.meta.SFXParser3;
import com.daquexian.chaoli.forum.utils.LoginUtils;
import com.daquexian.chaoli.forum.viewmodel.BaseViewModel;
import com.daquexian.chaoli.forum.viewmodel.PostActionVM;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jianhao on 16-5-31.
 * Activity for start a new conversation
 */
public class PostAction extends BaseActivity implements IView {

    private static final String TAG = "PostAction";

    public static final int MENU_POST = 2;
    public static final int MENU_DEMO = 1;

    private PostActionVM viewModel;
    private PostActionBinding binding;

    private ProgressDialog progressDialog;

    private List<View> expressionsIVList = new ArrayList<>();

    /* 切换至演示模式时保存光标位置，切换回普通模式时恢复 */
    private int selectionStart, selectionEnd;

    private BottomSheetBehavior behavior;

    private final Context mContext = this;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setViewModel(new PostActionVM());

        init();
    }

    private void init() {
        configToolbar(R.string.post);

        final String[] channelArr = {getString(R.string.channel_caff), getString(R.string.channel_maths),
                getString(R.string.channel_physics), getString(R.string.channel_chem),
                getString(R.string.channel_biology), getString(R.string.channel_tech), getString(R.string.channel_lang),
                getString(R.string.channel_socsci)};

        viewModel.updateContentRichText.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
            }
        });

        viewModel.updateTitleRichText.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
            }
        });

        viewModel.demoMode.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                if (((ObservableBoolean) observable).get()) {
                    selectionStart = binding.content.getSelectionStart();
                    selectionEnd = binding.content.getSelectionEnd();
                    binding.content.setEnabled(false);
                    binding.content.setOnlineImgEnabled(true);
                    binding.content.update();
                } else {
                    binding.content.setEnabled(true);
                    binding.content.setOnlineImgEnabled(false);
                    binding.content.update();
                    binding.content.setSelection(selectionStart, selectionEnd);
                }
            }
        });

        binding.title.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                viewModel.doAfterTitleChanged();
            }
        });

        View.OnFocusChangeListener onFocusChangeListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) tryToShowSoftKeyboard(view);
            }
        };

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tryToShowSoftKeyboard(view);
            }
        };

        binding.title.setOnFocusChangeListener(onFocusChangeListener);
        binding.content.setOnFocusChangeListener(onFocusChangeListener);
        binding.title.setOnClickListener(onClickListener);
        binding.content.setOnClickListener(onClickListener);

        binding.content.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                viewModel.doAfterContentChanged();
            }
        });

        binding.channel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(mContext).setTitle("选择板块").setItems(channelArr, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //noinspection ConstantConditions
                        viewModel.setChannelId(Channel.getChannel(channelArr[which]).getChannelId());
                    }
                }).setCancelable(false).show();
            }
        });

        viewModel.postComplete.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                setResult(RESULT_OK);
                finish();
            }
        });

        viewModel.showToast.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                showToast(viewModel.toastContent.get());
            }
        });

        viewModel.showWelcome.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                new AlertDialog.Builder(mContext).setMessage(R.string.welcome_to_demo_mode)
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
                if (((ObservableBoolean) observable).get()) progressDialog = ProgressDialog.show(PostAction.this, "", getString(R.string.just_a_sec));
                else progressDialog.dismiss();
            }
        });

        /*
         * 让各个表情按钮响应单击事件
         */

        View.OnClickListener onExpressionClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (behavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    final View focused = getCurrentFocus();
                    if (focused instanceof EditText) {
                        final EditText focusedET = (EditText) focused;
                        for (int j = 0; j < Constants.icons.length; j++) {
                            String icon = Constants.icons[j];
                            CharSequence contentDescription = view.getContentDescription();
                            if (icon.equals(contentDescription)) {
                                int start = Math.max(focusedET.getSelectionStart(), 0);
                                int end = Math.max(focusedET.getSelectionEnd(), 0);
                                focusedET.getText().replace(Math.min(start, end), Math.max(start, end),
                                        Constants.iconStrs[j], 0, Constants.iconStrs[j].length());
                                updateRichText();
                                break;
                            }
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
                expressionView.setOnClickListener(onExpressionClickListener);
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

        binding.title.requestFocus();
        tryToShowSoftKeyboard(binding.title);
    }

    private void updateRichText() {
        int selectionStart = binding.content.getSelectionStart();
        int selectionEnd = binding.content.getSelectionEnd();
        binding.content.setText(SFXParser3.parse(getApplicationContext(), viewModel.content.get(), null));
        binding.content.setSelection(selectionStart, selectionEnd);
        selectionStart = binding.title.getSelectionStart();
        selectionEnd = binding.title.getSelectionEnd();
        binding.title.setText(SFXParser3.parse(getApplicationContext(), viewModel.title.get(), null));
        binding.title.setSelection(selectionStart, selectionEnd);
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
        menu.add(Menu.NONE, Menu.NONE, MENU_POST, R.string.post).setIcon(R.drawable.ic_cab_done_mtrl_alpha).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        super.onOptionsItemSelected(item);
        switch (item.getOrder())
        {
            case MENU_POST:
                Log.d(TAG, "onOptionsItemSelected: ");
                if (!LoginUtils.isLoggedIn()){
                    showToast(R.string.please_login);
                    break;
                }
                viewModel.postConversation();
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
        this.viewModel = (PostActionVM) viewModel;
        binding = DataBindingUtil.setContentView(this, R.layout.post_action);
        binding.setViewModel(this.viewModel);
    }
}
