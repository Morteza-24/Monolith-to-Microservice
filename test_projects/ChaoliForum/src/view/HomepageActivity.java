package com.daquexian.chaoli.forum.view;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.databinding.ActivityHomepageBinding;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.utils.LoginUtils;
import com.daquexian.chaoli.forum.viewmodel.BaseViewModel;
import com.daquexian.chaoli.forum.viewmodel.HistoryFragmentVM;
import com.daquexian.chaoli.forum.viewmodel.HomepageVM;

/**
 * Activity for user's homepage
 * Created by daquexian on 16-4-14.
 */

public class HomepageActivity extends BaseActivity implements AppBarLayout.OnOffsetChangedListener{
    final String TAG = "HomePageActivity";
    final Context mContext = this;

    final int MENU_SETTING = 0;
    final int MENU_LOGOUT = 1;

    final int SETTING_CODE = 0;

    AppBarLayout mAppBarLayout;
    ViewPager mViewPager;
    TabLayout mTabLayout;

    ProgressDialog progressDialog;

    HistoryFragment mHistoryFragment;
    StatisticFragment mStatisticFragment;
    HistoryFragment mNotificationFragment;

    HomepageVM viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getIntent().getExtras();
        if(bundle == null){
            Log.e(TAG, "bundle can't be null");
            this.finish();
            return;
        }

        viewModel = new HomepageVM(bundle.getString("username", ""), bundle.getString("signature", null), bundle.getString("avatarSuffix", Constants.NONE), bundle.getInt("userId", -1),
                bundle.getBoolean("isSelf", false));
        setViewModel(viewModel);

        /*if("".equals(mUsername) || mUserId == -1){
            this.finish();
            return;
        }*/

        CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbarLayout.setTitle(viewModel.username.get());
        collapsingToolbarLayout.setExpandedTitleGravity(0x01 | 0x50);
        Toolbar toolbar = (Toolbar) findViewById(R.id.tl_custom);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        //final SwipyRefreshLayout mSwipyRefreshLayout = (SwipyRefreshLayout) findViewById(R.id.mSwipyRefreshLayout);
        mAppBarLayout = (AppBarLayout) findViewById(R.id.appbar);
        mTabLayout = (TabLayout) findViewById(R.id.tabs);
        mViewPager = (ViewPager) findViewById(R.id.viewpager);

        mViewPager.setAdapter(new MyViewPagerAdapter(getSupportFragmentManager()));
        mViewPager.setOffscreenPageLimit(2);
        mTabLayout.addTab(mTabLayout.newTab().setText(viewModel.isSelf.get() ? R.string.notification : R.string.activity));
        mTabLayout.addTab(mTabLayout.newTab().setText(viewModel.isSelf.get() ? R.string.activity : R.string.statistics));
        if(viewModel.isSelf.get()) mTabLayout.addTab(mTabLayout.newTab().setText(R.string.statistics));
        mTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        final TabLayout.TabLayoutOnPageChangeListener listener =
                new TabLayout.TabLayoutOnPageChangeListener(mTabLayout);
        mViewPager.addOnPageChangeListener(listener);
        //mTabLayout.setupWithViewPager(mViewPager);
        mTabLayout.setTabMode(TabLayout.MODE_FIXED);

    }

    public class MyViewPagerAdapter extends FragmentPagerAdapter {
        MyViewPagerAdapter(FragmentManager fm){
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position){
                case 0:
                    if(viewModel.isSelf.get()) {
                        return addNotificationFragment();
                    }else {
                        return addHistoryFragment();
                    }
                case 1:
                    if(viewModel.isSelf.get()) {
                        return addHistoryFragment();
                    }else {
                        return addStatisticFragment();
                    }
                case 2:
                    return addStatisticFragment();
            }
            return null;
        }

        @Override
        public int getCount() {
            return viewModel.isSelf.get() ? 3 : 2;
        }

        private Fragment addNotificationFragment() {
            mNotificationFragment = new HistoryFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("type", HistoryFragmentVM.TYPE_NOTIFICATION);
            mNotificationFragment.setArguments(bundle);
            return mNotificationFragment;
        }
        private Fragment addHistoryFragment() {
            mHistoryFragment = new HistoryFragment();
            Bundle args0 = new Bundle();
            args0.putInt("type", HistoryFragmentVM.TYPE_ACTIVITY);
            args0.putInt("userId", viewModel.userId.get());
            args0.putString("username", viewModel.username.get());
            args0.putString("avatarSuffix", viewModel.avatarSuffix.get());
            mHistoryFragment.setArguments(args0);
            return mHistoryFragment;
        }

        private Fragment addStatisticFragment() {
            mStatisticFragment = new StatisticFragment();
            Bundle args1 = new Bundle();
            args1.putInt("userId", viewModel.userId.get());
            mStatisticFragment.setArguments(args1);
            return mStatisticFragment;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if(viewModel.isSelf.get()) {
            menu.add(Menu.NONE, Menu.NONE, MENU_SETTING, R.string.config).setIcon(R.drawable.ic_account_settings_variant).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.add(Menu.NONE, Menu.NONE, MENU_LOGOUT, R.string.logout).setIcon(R.drawable.ic_logout).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getOrder()){
            case MENU_SETTING:
                //startActivity(new Intent(HomepageActivity.this, SettingsActivity.class));
                startActivityForResult(new Intent(HomepageActivity.this, SettingsActivity.class), SETTING_CODE);
                break;
            case MENU_LOGOUT:
                // showProcessDialog(getString(R.string.just_a_sec));
                Toast.makeText(getApplicationContext(), R.string.logout_success, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(HomepageActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                LoginUtils.logout(new LoginUtils.LogoutObserver() {
                    @Override
                    public void onLogoutSuccess() {
                        // dismissProcessDialog();
                    }

                    @Override
                    public void onLogoutFailure(int statusCode) {
                        dismissProcessDialog();
                        showToast(R.string.network_err);
                    }
                });

                break;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case SETTING_CODE:
                if (resultCode == RESULT_OK) viewModel.updateSelfProfile();
                break;
        }
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        //The Refresh must be only active when the offset is zero :
        mHistoryFragment.setRefreshEnabled(i == 0);
        if(mNotificationFragment != null) mNotificationFragment.setRefreshEnabled(i == 0);
    }

    public void showProcessDialog(String str) {
        progressDialog = ProgressDialog.show(this, "", str);
    }

    public void dismissProcessDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAppBarLayout.addOnOffsetChangedListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAppBarLayout.removeOnOffsetChangedListener(this);
    }

    @Override
    public void setViewModel(BaseViewModel viewModel) {
        this.viewModel = (HomepageVM) viewModel;
        ActivityHomepageBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_homepage);
        binding.setViewModel(this.viewModel);
    }
}