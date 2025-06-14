package com.daquexian.chaoli.forum.view;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.utils.LoginUtils;
import com.daquexian.chaoli.forum.viewmodel.BaseViewModel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Entry point of whole application
 * parse link(if there is one) and distribute to corresponding activity, or start MainActivity directly
 * Created by daquexian on 17-1-28.
 */

public class EntryPointActivity extends BaseActivity {
    private static final String TAG = "EntryPointActivity";

    private static final String BASE_PATTERN = "https://(www.)?chaoli.club/(index.php)?$";
    private static final String CONVERSATION_PATTERN = "https://(www.)?chaoli.club/index.php/(\\d+)$";
    private static final String HOMEPAGE_PATTERN = "https://(www.)?chaoli.club/index.php/member/(\\d+)$";

    private static final int REQUEST_PERMISSION_CODE = 1;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry_point);

        configToolbar(R.string.app_name);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.need_write_permission))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(EntryPointActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE);
                        }
                    })
                    .show();
        } else {
            core();
        }

    }

    private void core() {
        Intent intent = getIntent();
        Uri uri = intent.getData();

        if (uri == null) {
            Intent goToMainActivity = new Intent(this, MainActivity.class);
            startActivityWithoutAnimation(goToMainActivity);
        } else {
            final String url = uri.toString();
            Log.d(TAG, "onCreate: url = " + url);

            final Pattern[] pattern = {Pattern.compile(BASE_PATTERN)};
            final Matcher[] matcher = {pattern[0].matcher(url)};

            if (matcher[0].find()) {
                Intent goToMainActivity = new Intent(this, MainActivity.class);
                startActivityWithoutAnimation(goToMainActivity);
            } else {
                final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.srl);
                swipeRefreshLayout.setRefreshing(true);

                LoginUtils.begin_login(new LoginUtils.LoginObserver() {
                    @Override
                    public void onLoginSuccess(int userId, String token) {

                        pattern[0] = Pattern.compile(CONVERSATION_PATTERN);
                        matcher[0] = pattern[0].matcher(url);

                        if (matcher[0].find()) {
                            String conversationId = matcher[0].group(2);
                            Intent goToPostActivity = new Intent(EntryPointActivity.this, PostActivity.class);
                            goToPostActivity.putExtra("conversationId", Integer.parseInt(conversationId));
                            startActivityWithoutAnimation(goToPostActivity);
                        } else {
                            showToast(R.string.not_support_temporarily);
                            finish();
                        }

                /*
                pattern = Pattern.compile(HOMEPAGE_PATTERN);
                matcher = pattern.matcher(url);

                if (matcher.find()) {
                    String memberId = matcher.group(2);
                    Intent goToHomepageActivity = new Intent(EntryPointActivity.this, HomepageActivity.class);

                }*/
                    }

                    @Override
                    public void onLoginFailure(int statusCode) {
                        swipeRefreshLayout.setRefreshing(false);
                        showToast(R.string.network_err);
                        finish();
                    }
                });
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        core();
    }

    private void startActivityWithoutAnimation(Intent intent) {
        // getWindow().setWindowAnimations(0);
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
    }

    @Override
    public void setViewModel(BaseViewModel viewModel) {
        // empty method
    }
}
