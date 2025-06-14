package com.daquexian.chaoli.forum.view;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.databinding.Observable;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableInt;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.data.Me;
import com.daquexian.chaoli.forum.databinding.MainActivityBinding;
import com.daquexian.chaoli.forum.databinding.NavigationHeaderBinding;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.meta.NightModeHelper;
import com.daquexian.chaoli.forum.model.Conversation;
import com.daquexian.chaoli.forum.utils.DataBindingUtils;
import com.daquexian.chaoli.forum.utils.MyUtils;
import com.daquexian.chaoli.forum.viewmodel.BaseViewModel;
import com.daquexian.chaoli.forum.viewmodel.MainActivityVM;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

public class MainActivity extends BaseActivity implements AppBarLayout.OnOffsetChangedListener
{
	private static final String LAYOUTMANAGER＿STATE = "layoutManager";
	private static final String TOOLBAR_OFFSET = "toolbar_offset";
	public static final String TAG = "MainActivity";

	public Toolbar toolbar;
	public DrawerLayout mDrawerLayout;
	public LinearLayoutManager layoutManager;
	public Parcelable layoutManagerState = null;

	private Context mContext = this;

	private ProgressDialog loginProgressDialog;

	private final int POST_CONVERSATION_CODE = 1;
	private final int LOGIN_CODE = 2;

	/**
	 * ATTENTION: This was auto-generated to implement the App Indexing API.
	 * See https://g.co/AppIndexing/AndroidStudio for more information.
	 */
	private GoogleApiClient client;

	RecyclerView l;

	public SwipyRefreshLayout swipyRefreshLayout;

	ActionBarDrawerToggle actionBarDrawerToggle;

	MainActivityVM viewModel;
	MainActivityBinding binding;

	boolean bottom = true;	//是否滚到底部
	boolean needTwoClick = false;
	boolean clickedOnce = false;	//点击Back键两次退出

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putParcelable(LAYOUTMANAGER＿STATE,layoutManager.onSaveInstanceState());
		outState.putInt(TOOLBAR_OFFSET, binding.appbar.getOffset());
		super.onSaveInstanceState(outState);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (NightModeHelper.getViewModel() == null) {
			viewModel = new MainActivityVM();
			setViewModel(viewModel);
			addCallbacks();
			viewModel.setChannel("all");
			viewModel.startUp();
		} else {
			viewModel = (MainActivityVM) NightModeHelper.getViewModel();	// remove the reference to ViewModel in NightModeHelper later in initUI
			setViewModel(viewModel);
			addCallbacks();
		}

		initUI(savedInstanceState);

		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
	}

	private void addCallbacks() {
		DataBindingUtils.addCallback(this, viewModel.goToLogin, new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				goToLogin();
			}
		});

		DataBindingUtils.addCallback(this, viewModel.goToHomepage, new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				goToMyHomePage();
			}
		});

		DataBindingUtils.addCallback(this, viewModel.goToConversation, new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				goToConversation(viewModel.clickedConversation);
			}
		});

		DataBindingUtils.addCallback(this, viewModel.notificationsNum, new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				if (((ObservableInt) observable).get() > 0) {
					setCircleIndicator();
				} else {
					setNormalIndicator();
				}
			}
		});

		DataBindingUtils.addCallback(this, viewModel.showLoginProcessDialog, new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				if (((ObservableBoolean) observable).get()) {
					showLoginProcessDialog();
				} else {
					dismissLoginProcessDialog();
				}
			}
		});

		DataBindingUtils.addCallback(this, viewModel.showToast, new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				showToast(viewModel.toastContent);
			}
		});

		DataBindingUtils.addCallback(this, viewModel.selectedItem, new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				selectItem(((ObservableInt) observable).get());
			}
		});

		DataBindingUtils.addCallback(this, viewModel.toFirstLoadConversation, new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				selectItem(0, false);
			}
		});

		DataBindingUtils.addCallback(this, viewModel.goToPost, new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				goToPostAction();
			}
		});

		DataBindingUtils.addCallback(this, viewModel.failed, new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				if (((ObservableBoolean) observable).get()) showToast(R.string.network_err);
			}
		});

		DataBindingUtils.addCallback(this, viewModel.smoothToFirst, new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable sender, int propertyId) {
				l.smoothScrollToPosition(0);
			}
		});

		/**
		 * 根据登录状态更改侧栏菜单
		 */
		DataBindingUtils.addCallback(this, viewModel.isLoggedIn, new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				binding.navigationView.getMenu().clear();
				binding.navigationView.inflateMenu(viewModel.isLoggedIn.get() ? R.menu.menu_navigation : R.menu.menu_navigation_no_login);
			}
		});

	}

	public void selectItem(int position) {
		selectItem(position, true);
	}

	public void selectItem(int position, boolean closeDrawers) {
		viewModel.setChannel(viewModel.getChannelByPosition(position));
		viewModel.refresh();
		if (closeDrawers) {
			mDrawerLayout.closeDrawers();
		}
	}

	public void initUI(Bundle savedInstanceState) {
		Toolbar toolbar = (Toolbar) findViewById(R.id.tl_custom);
		configToolbar(R.string.app_name);

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

		actionBarDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.app_name, R.string.app_name);
		actionBarDrawerToggle.setDrawerIndicatorEnabled(false);
		actionBarDrawerToggle.setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp);
		actionBarDrawerToggle.setToolbarNavigationClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mDrawerLayout.openDrawer(GravityCompat.START);
			}
		});
		actionBarDrawerToggle.syncState();
		mDrawerLayout.addDrawerListener(actionBarDrawerToggle);

		l = binding.conversationList;
		l.addItemDecoration(new android.support.v7.widget.DividerItemDecoration(mContext, android.support.v7.widget.DividerItemDecoration.VERTICAL));

		layoutManager = new LinearLayoutManager(mContext);
		l.setLayoutManager(layoutManager);
		if (NightModeHelper.getViewModel() != null) {
			layoutManager.onRestoreInstanceState(savedInstanceState.getParcelable(LAYOUTMANAGER＿STATE));
		}

		swipyRefreshLayout = binding.conversationListRefreshLayout;

		if (NightModeHelper.getViewModel() != null) {
			MyUtils.setToolbarOffset(binding.cl, binding.appbar, savedInstanceState.getInt(TOOLBAR_OFFSET));
		}

		binding.appbar.addOnOffsetChangedListener(this);
		binding.conversationList.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
				//得到当前显示的最后一个item的view
                View lastChildView = recyclerView.getLayoutManager().getChildAt(recyclerView.getLayoutManager().getChildCount()-1);
				if (lastChildView == null) return;
                //得到lastChildView的bottom坐标值
                int lastChildBottom = lastChildView.getBottom();
                //得到Recyclerview的底部坐标减去底部padding值，也就是显示内容最底部的坐标
                int recyclerBottom =  recyclerView.getBottom()-recyclerView.getPaddingBottom();
                //通过这个lastChildView得到这个view当前的position值
                int lastVisiblePosition  = layoutManager.findLastVisibleItemPosition();

                //判断lastChildView的bottom值跟recyclerBottom
                //判断lastPosition是不是最后一个position
                //如果两个条件都满足则说明是真正的滑动到了底部
				int lastPosition = recyclerView.getLayoutManager().getItemCount() - 1;
				if(lastChildBottom == recyclerBottom && lastVisiblePosition == lastPosition){
                    bottom = true;
                    viewModel.canRefresh.set(true);
                }else{
                    bottom = false;
                }
				if (lastVisiblePosition >= lastPosition - 3) viewModel.tryToLoadFromBottom();
			}
		});

		final NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
		navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(@NonNull MenuItem item) {
				if (item.getItemId()==R.id.nightMode){
					NightModeHelper.changeMode(viewModel);
					getWindow().setWindowAnimations(R.style.modechange);
					recreate();
				}else {
					selectItem(item.getOrder());
					item.setChecked(true);
				}
				return true;
			}
		});

		swipyRefreshLayout.setDirection(SwipyRefreshLayoutDirection.BOTH);

		swipyRefreshLayout.setOnRefreshListener(new SwipyRefreshLayout.OnRefreshListener()
		{
			@Override
			public void onRefresh(SwipyRefreshLayoutDirection direction)
			{
				if (direction == SwipyRefreshLayoutDirection.TOP) {
					viewModel.refresh();
				} else {
					viewModel.loadMore();
				}
			}
		});

		NightModeHelper.removeViewModel();
	}

	@Override
	public void onBackPressed() {
		if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) mDrawerLayout.closeDrawer(GravityCompat.START);
		else if (clickedOnce || !needTwoClick) super.onBackPressed();
		else {
			showToast(R.string.click_once_more_to_exit);
			clickedOnce = true;
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					clickedOnce = false;
				}
			}, 2500);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy() called");
		DataBindingUtils.removeCallbacks(this);
		viewModel.destory();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (layoutManagerState!=null){
			layoutManager.onRestoreInstanceState(layoutManagerState);
		}
		viewModel.resume();
		needTwoClick = getSharedPreferences(Constants.SETTINGS_SP, MODE_PRIVATE).getBoolean(Constants.CLICK_TWICE_TO_EXIT, false);
	}

	@Override
	public void onStart() {
		super.onStart();

		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		client.connect();
		Action viewAction = Action.newAction(
				Action.TYPE_VIEW, // TODO: choose an action type.
				"Main Page", // TODO: Define a mTitle for the content shown.
				// TODO: If you have web page content that matches this app activity's content,
				// make sure this auto-generated web page URL is correct.
				// Otherwise, set the URL to null.
				Uri.parse("http://host/path"),
				// TODO: Make sure this auto-generated app URL is correct.
				Uri.parse("android-app://com.daquexian.chaoli.forum/http/host/path")
		);
		AppIndex.AppIndexApi.start(client, viewAction);
	}

	@Override
	public void onStop() {
		super.onStop();

		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		Action viewAction = Action.newAction(
				Action.TYPE_VIEW, // TODO: choose an action type.
				"Main Page", // TODO: Define a mTitle for the content shown.
				// TODO: If you have web page content that matches this app activity's content,
				// make sure this auto-generated web page URL is correct.
				// Otherwise, set the URL to null.
				Uri.parse("http://host/path"),
				// TODO: Make sure this auto-generated app URL is correct.
				Uri.parse("android-app://com.daquexian.chaoli.forum/http/host/path")
		);
		AppIndex.AppIndexApi.end(client, viewAction);
		client.disconnect();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
			case POST_CONVERSATION_CODE:
				if (resultCode == RESULT_OK) {
					viewModel.refresh();
				}
				break;
			case LOGIN_CODE:
				if (resultCode == RESULT_OK) {
					viewModel.refresh();
					viewModel.getProfile();
					viewModel.loginComplete.set(true);
					viewModel.isLoggedIn.set(true);
					viewModel.myUsername.set(getString(R.string.loading));
					viewModel.mySignature.set(getString(R.string.loading));
				}
				break;
		}
	}

	public void goToConversation(Conversation conversation) {
		conversation.setUnread("0");
		Intent jmp = new Intent();
		jmp.putExtra("conversation", conversation);
		//jmp.putExtra("conversationId", conversation.getConversationId());
		//jmp.putExtra("conversationTitle", conversation.getTitle());
		jmp.setClass(this, PostActivity.class);
		startActivity(jmp);
	}

	public void goToMyHomePage() {
		Log.d(TAG, "goToMyHomePage: " + Me.isEmpty());
		if(viewModel.isLoggedIn.get()){
			if(!Me.isEmpty()) {
				Intent intent = new Intent(this, HomepageActivity.class);
				Bundle bundle = new Bundle();
				bundle.putString("username", Me.getUsername());
				bundle.putInt("userId", Me.getUserId());
				bundle.putString("signature", Me.getPreferences().getSignature());
				bundle.putString("avatarSuffix", Me.getAvatarSuffix() == null ? Constants.NONE : Me.getAvatarSuffix());
				bundle.putBoolean("isSelf",  true);
				intent.putExtras(bundle);
				startActivity(intent);
			}
		}
	}

	public void goToLogin() {
		// startActivity(new Intent(this, LoginActivity.class));
		startActivityForResult(new Intent(this, LoginActivity.class), LOGIN_CODE);
	}

	public void goToPostAction() {
		Intent intent = new Intent(this, PostAction.class);
		startActivityForResult(intent, POST_CONVERSATION_CODE);
	}

	public void setCircleIndicator() {
		actionBarDrawerToggle.setHomeAsUpIndicator(R.drawable.ic_menu_black_with_a_circle_24dp);
		actionBarDrawerToggle.syncState();
	}

	public void setNormalIndicator() {
		actionBarDrawerToggle.setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp);
		actionBarDrawerToggle.syncState();
	}

	public void showLoginProcessDialog() {
		loginProgressDialog = ProgressDialog.show(this, "", getResources().getString(R.string.logging_in));
	}

	public void dismissLoginProcessDialog() {
		loginProgressDialog.dismiss();
	}

	@Override
	public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
		/**
		 * verticalOffset == 0说明appbar已经是展开状态
		 */
		viewModel.canRefresh.set(verticalOffset == 0 || bottom);
	}

	@Override
	public void setViewModel(BaseViewModel viewModel) {
		this.viewModel = (MainActivityVM) viewModel;
		binding = DataBindingUtil.setContentView(this, R.layout.main_activity);
		binding.setViewModel(this.viewModel);
		NavigationHeaderBinding navigationHeaderBinding = NavigationHeaderBinding.bind(binding.navigationView.getHeaderView(0));
		navigationHeaderBinding.setViewModel(this.viewModel);
	}
}
