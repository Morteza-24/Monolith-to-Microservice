
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.lincanbin.carbonforum.adapter.PostAdapter;
import com.lincanbin.carbonforum.application.CarbonForumApplication;
import com.lincanbin.carbonforum.config.APIAddress;
import com.lincanbin.carbonforum.util.HttpUtil;
import com.lincanbin.carbonforum.util.JSONUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TopicActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {
    private Toolbar mToolbar;
    private TextView mTopicTitle;
    private RecyclerView mRecyclerView ;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private FloatingActionButton mFloatingActionButton;
    private PostAdapter mAdapter;
    private String mTopic;
    private String mTopicID;
    private String mTopicPage;
    private int currentPage = 0;
    private int totalPage = 65536;
    private Boolean enableScrollListener = true;
    private List<Map<String,Object>> postList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //注册一个广播用于回复成功时，刷新主题
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("action.refreshTopic");
        LocalBroadcastManager.getInstance(this).registerReceiver(mRefreshTopicBroadcastReceiver, intentFilter);
        //取得启动该Activity的Intent对象
        Intent mIntent = getIntent();
        //取出Intent中附加的数据
        mTopic = mIntent.getStringExtra("Topic");
        mTopicID = mIntent.getStringExtra("TopicID");
        mTopicPage = mIntent.getStringExtra("TargetPage");
        setContentView(R.layout.activity_topic);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if(mTopic != null) {
            getSupportActionBar().setTitle(mTopic);
        }
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.activity_topic_swipe_refresh_layout);
        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.material_light_blue_700,
                R.color.material_red_700,
                R.color.material_orange_700,
                R.color.material_light_green_700
        );
        mSwipeRefreshLayout.setOnRefreshListener(this);
        /*
        if(Integer.parseInt(mTopicPage) == 1) {
            mTopicTitle = (TextView) findViewById(R.id.title);
            mTopicTitle.setText(mTopic);
        }
        */
        //RecyclerView
        mRecyclerView = (RecyclerView) findViewById(R.id.post_list);
        mRecyclerView.setHasFixedSize(true);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    int lastVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition();
                    int totalItemCount = layoutManager.getItemCount();
                    if (lastVisibleItem >= (totalItemCount - 5) && enableScrollListener && currentPage < totalPage) {
                        loadPost(currentPage + 1);
                    }
                }
            }
        });
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mAdapter = new PostAdapter(this, false);
        mAdapter.setData(postList);
        mRecyclerView.setAdapter(mAdapter);

        mFloatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(TopicActivity.this, ReplyActivity.class);
                intent.putExtra("TopicID", mTopicID);
                intent.putExtra("PostID", "0");
                intent.putExtra("PostFloor", "0");
                intent.putExtra("UserName", "0");
                intent.putExtra("DefaultContent", "");
                startActivity(intent);
            }
        });
        if(!CarbonForumApplication.isLoggedIn()){
            mFloatingActionButton.setVisibility(View.INVISIBLE);
        }
        loadPost(Integer.parseInt(mTopicPage));
    }
    //加载帖子
    private void loadPost(int targetPage) {
        new GetPostTask(targetPage).execute();
    }
    // broadcast receiver
    private BroadcastReceiver mRefreshTopicBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int tempTargetPage = intent.getIntExtra("TargetPage",1);
            if (action.equals("action.refreshTopic") && tempTargetPage == 1) {
                loadPost(1);
            }
        }
    };
    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRefreshTopicBroadcastReceiver);
    }
    //下拉刷新事件
    @Override
    public void onRefresh() {
        //if(!mSwipeRefreshLayout.isRefreshing()){
            loadPost(1);
        //}
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // API 5+ solution
                onBackPressed();
                //NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    public class GetPostTask extends AsyncTask<Void, Void, JSONObject> {
        private int targetPage;
        private int positionStart;
        public GetPostTask(int targetPage) {
            this.targetPage = targetPage;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            enableScrollListener = false;
            mSwipeRefreshLayout.post(new Runnable(){
                @Override
                public void run(){
                    mSwipeRefreshLayout.setRefreshing(true);
                }
            });
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            super.onPostExecute(jsonObject);
            //Log.v("JSON", str);
            if(jsonObject != null){
                try {
                    totalPage = jsonObject.getInt("TotalPage");
                    JSONObject topicInfo = JSONUtil.jsonString2Object(jsonObject.getString("TopicInfo"));
                    getSupportActionBar().setTitle(topicInfo.getString("Topic"));
                }catch(JSONException e){
                    e.printStackTrace();
                }
            }
            List<Map<String,Object>> list = JSONUtil.jsonObject2List(jsonObject, "PostsArray");
            //Log.v("List", list.toString());
            if(list!=null && !list.isEmpty()) {

                if (targetPage > 1) {
                    positionStart = postList.size() - 1;
                    postList.addAll(list);
                    mAdapter.setData(postList);
                    mAdapter.notifyItemRangeChanged(positionStart, mAdapter.getItemCount());
                } else {
                    postList = list;
                    mAdapter.setData(postList);
                    mAdapter.notifyDataSetChanged();
                }
                //更新当前页数
                currentPage = targetPage;
            }else{
                Snackbar.make(mFloatingActionButton, R.string.network_error, Snackbar.LENGTH_LONG).setAction("Action", null).show();
                //Toast.makeText(TopicActivity.this, R.string.network_error, Toast.LENGTH_SHORT).show();
            }
            //移除刷新控件
            mSwipeRefreshLayout.setRefreshing(false);
            enableScrollListener = true;
            //Toast.makeText(IndexActivity.this, "AsyncTask End", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            JSONObject temp = HttpUtil.postRequest(TopicActivity.this, APIAddress.TOPIC_URL(Integer.parseInt(mTopicID), targetPage), null, false, true);
            //Log.v("TopicJSON", temp.toString());
            return temp;
        }

    }
}


import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import com.lincanbin.carbonforum.service.NewService;
import com.lincanbin.carbonforum.util.markdown.MarkdownProcessor;

public class NewActivity extends AppCompatActivity {
    Toolbar mToolbar;
    EditText mTitle;
    EditText mTag;
    EditText mContent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mTitle = (EditText) findViewById(R.id.title);
        mTag = (EditText) findViewById(R.id.tag);
        mContent = (EditText) findViewById(R.id.content);
        if (mToolbar != null) {
            mToolbar.setTitle(R.string.title_activity_new);
            setSupportActionBar(mToolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            ImageButton imageButton = (ImageButton) mToolbar.findViewById(R.id.new_button);
            imageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mTitle.getText().toString().length() > 0 &&
                            mTag.getText().toString().replace("，",",").split(",").length > 0) {
                        MarkdownProcessor mMarkdownProcessor = new MarkdownProcessor();
                        String contentHTML = mMarkdownProcessor.markdown(mContent.getText().toString());
                        Intent intent = new Intent(NewActivity.this, NewService.class);
                        intent.putExtra("Title", mTitle.getText().toString());
                        intent.putExtra("Tag", mTag.getText().toString());
                        intent.putExtra("Content", contentHTML);
                        startService(intent);
                        onBackPressed();
                    }else{
                        Snackbar.make(view, getString(R.string.content_empty), Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    }
                }
            });
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.lincanbin.carbonforum.adapter.TopicAdapter;
import com.lincanbin.carbonforum.application.CarbonForumApplication;
import com.lincanbin.carbonforum.config.APIAddress;
import com.lincanbin.carbonforum.service.PushService;
import com.lincanbin.carbonforum.util.HttpUtil;
import com.lincanbin.carbonforum.util.JSONUtil;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.holder.BadgeStyle;
import com.mikepenz.materialdrawer.holder.StringHolder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.util.RecyclerViewCacheUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//http://stackoverflow.com/questions/28150100/setsupportactionbar-throws-error/28150167
public class IndexActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {
    private Toolbar mToolbar;
    //save our header or result
    private AccountHeader headerResult = null;
    private Drawer mDrawer = null;
    private RecyclerView mRecyclerView ;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private FloatingActionButton mFloatingActionButton;
    private TopicAdapter mAdapter;
    //private SharedPreferences mSharedPreferences;
    //private ActionBarDrawerToggle mDrawerToggle;
    private int currentPage = 0;
    private int totalPage = 65536;
    private Boolean enableScrollListener = true;
    private List<Map<String,Object>> topicList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_index);
        //注册一个广播用来登录和退出时刷新Drawer
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("action.refreshDrawer");
        LocalBroadcastManager.getInstance(this).registerReceiver(mRefreshDrawerBroadcastReceiver, intentFilter);
        //mSharedPreferences = getSharedPreferences("UserInfo", Activity.MODE_PRIVATE);
        // 设置ToolBar
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mFloatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(IndexActivity.this, NewActivity.class);
                startActivity(intent);

            }
        });
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);//把Toolbar当做ActionBar给设置了
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setTitle(R.string.app_name);
            //mToolbar.bringToFront();
            //toolbar.setLogo(R.drawable.ic_launcher);
            // toolbar.setSubtitle("Sub title");

            refreshDrawer(savedInstanceState);
        }
        //下拉刷新监听器
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.activity_index_swipe_refresh_layout);
        //设置刷新时动画的颜色，可以设置4个
        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.material_light_blue_700,
                R.color.material_red_700,
                R.color.material_orange_700,
                R.color.material_light_green_700
        );
        mSwipeRefreshLayout.setOnRefreshListener(this);
        //RecyclerView
        mRecyclerView = (RecyclerView) findViewById(R.id.topic_list);
        //使RecyclerView保持固定的大小，这样会提高RecyclerView的性能
        mRecyclerView.setHasFixedSize(true);
        // 创建一个线性布局管理器
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        //setOnScrollListener已废弃，使用addOnScrollListener需要在使用后用clearOnScrollListeners()移除监听器
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                // 当不滚动时
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    //获取最后一个完全显示的ItemPosition
                    int lastVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition();
                    int totalItemCount = layoutManager.getItemCount();
                    // 判断是否滚动到底部，并且是向右滚动
                    if (lastVisibleItem >= (totalItemCount - 5) && enableScrollListener && currentPage < totalPage) {
                        //加载更多功能的代码
                        loadTopic(currentPage + 1, false);
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                //dx用来判断横向滑动方向，dy用来判断纵向滑动方向
                /*
                if (dx > 0) {
                    //大于0表示，正在向右滚动
                } else {
                    //小于等于0 表示停止或向左滚动
                }
                */
            }
        });
        // 设置布局管理器
        mRecyclerView.setLayoutManager(layoutManager);
        //设置Item默认动画
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        //指定数据集
        mAdapter = new TopicAdapter(this);
        mAdapter.setData(topicList);
        //设置Adapter
        mRecyclerView.setAdapter(mAdapter);
        //Activity渲染完毕时加载帖子，使用缓存
        loadTopic(1, true);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRefreshDrawerBroadcastReceiver);
    }
    //加载帖子列表
    private void loadTopic(int targetPage, Boolean enableCache) {
        new GetTopicsTask(targetPage, enableCache).execute();
    }
    // broadcast receiver
    private BroadcastReceiver mRefreshDrawerBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("action.refreshDrawer")) {
                refreshDrawer(null);
            }
        }
    };
    private void refreshDrawer(Bundle savedInstanceState){
        try{
            //Log.v("UserID", mSharedPreferences.getString("UserID", "0"));
            if(!CarbonForumApplication.isLoggedIn()){ //未登录
                //隐藏发帖按钮
                mFloatingActionButton.setVisibility(View.INVISIBLE);
                final IProfile profile = new ProfileDrawerItem()
                        .withName("Not logged in")
                        .withIcon(R.drawable.profile)
                        .withIdentifier(0);
                // Create the AccountHeader
                headerResult = new AccountHeaderBuilder()
                        .withActivity(this)
                        .withHeaderBackground(R.drawable.header)
                        .withSelectionListEnabledForSingleProfile(false)
                        .addProfiles(
                                profile
                        )
                        .withSavedInstance(savedInstanceState)
                        .build();
            }else{ //已登录
                //显示发帖按钮
                mFloatingActionButton.setVisibility(View.VISIBLE);
                final IProfile profile = new ProfileDrawerItem()
                        .withName(CarbonForumApplication.userInfo.getString("UserName", "lincanbin"))
                        .withEmail(CarbonForumApplication.userInfo.getString("UserMail", CarbonForumApplication.userInfo.getString("UserName", "lincanbin")))
                        .withIcon(Uri.parse(APIAddress.MIDDLE_AVATAR_URL(CarbonForumApplication.userInfo.getString("UserID", "0"), "large")))
                        .withIdentifier(Integer.parseInt(CarbonForumApplication.userInfo.getString("UserID", "0")));
                // Create the AccountHeader
                headerResult = new AccountHeaderBuilder()
                        .withActivity(this)
                        .withHeaderBackground(R.drawable.header)
                        .withSelectionListEnabledForSingleProfile(false)
                                //.withTranslucentStatusBar(false)
                        .addProfiles(
                                profile,
                                //don't ask but google uses 14dp for the add account icon in gmail but 20dp for the normal icons (like manage account)
                                new ProfileSettingDrawerItem()
                                        .withName(getString(R.string.change_account))
                                        .withIcon(GoogleMaterial.Icon.gmd_accounts)
                                        .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                                            @Override
                                            public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                                                IndexActivity.this.startActivity(new Intent(IndexActivity.this, LoginActivity.class));
                                                return false;
                                            }
                                        }),
                                new ProfileSettingDrawerItem()
                                        .withName(getString(R.string.log_out))
                                        .withIcon(GoogleMaterial.Icon.gmd_close)
                                        .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                                            @Override
                                            public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                                                CarbonForumApplication.userInfo.edit().clear().apply();
                                                refreshDrawer(null);
                                                return false;
                                            }
                                        })
                        )
                        .withSavedInstance(savedInstanceState)
                        .build();
                //开启推送
                startService(new Intent(IndexActivity.this, PushService.class));
            }
        } catch (ClassCastException e) {
            e.printStackTrace();
        }

        //Create the drawer
        DrawerBuilder mDrawerBuilder = new DrawerBuilder()
                .withActivity(this)
                .withActionBarDrawerToggle(true)
                .withToolbar(mToolbar)
                .withHasStableIds(true)
                .withAccountHeader(headerResult) //set the AccountHeader we created earlier for the header
                .withSavedInstance(savedInstanceState)
                .withShowDrawerOnFirstLaunch(true)
                        // .withTranslucentStatusBar(false)
                .addDrawerItems(
                        new PrimaryDrawerItem().
                                withName(R.string.app_name).
                                withIcon(GoogleMaterial.Icon.gmd_home).
                                withSetSelected(true).
                                withIdentifier(1).
                                withSelectable(true),
                        new PrimaryDrawerItem().
                                withName(R.string.refresh).
                                withIcon(GoogleMaterial.Icon.gmd_refresh).
                                withIdentifier(2).
                                withSelectable(false),
                        new DividerDrawerItem()
                ) // add the items we want to use with our Drawer
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        //check if the drawerItem is set.
                        //there are different reasons for the drawerItem to be null
                        //--> click on the header
                        //--> click on the footer
                        //those items don't contain a drawerItem
                        if (drawerItem != null) {
                            Intent intent = null;
                            if (drawerItem.getIdentifier() == 2) {
                                loadTopic(1, false);
                            } else if (drawerItem.getIdentifier() == 3) {
                                intent = new Intent(IndexActivity.this, LoginActivity.class);
                            } else if (drawerItem.getIdentifier() == 4) {
                                intent = new Intent(IndexActivity.this, RegisterActivity.class);
                            } else if (drawerItem.getIdentifier() == 5) {
                                intent = new Intent(IndexActivity.this, NotificationsActivity.class);
                            } else if (drawerItem.getIdentifier() == 6) {
                                intent = new Intent(IndexActivity.this, SettingsActivity.class);
                            }
                            if (intent != null) {
                                IndexActivity.this.startActivity(intent);
                            }
                        }

                        return false;
                    }
                });


        if(!CarbonForumApplication.isLoggedIn()) { //未登录
            mDrawerBuilder.addDrawerItems(
                    new PrimaryDrawerItem().
                            withName(R.string.title_activity_login).
                            withIcon(GoogleMaterial.Icon.gmd_account).
                            withIdentifier(3).
                            withSelectable(false),
                    new PrimaryDrawerItem().
                            withName(R.string.title_activity_register).
                            withIcon(GoogleMaterial.Icon.gmd_account_add).
                            withIdentifier(4).
                            withSelectable(false)
            );
        }else{ //已登录
            mDrawerBuilder.addDrawerItems(
                    new PrimaryDrawerItem()
                            .withName(R.string.title_activity_notifications)
                            .withIcon(GoogleMaterial.Icon.gmd_notifications)
                            .withIdentifier(5)
                            .withBadgeStyle(new BadgeStyle().withTextColor(Color.WHITE).withColorRes(R.color.md_red_700))
                            .withSelectable(false),
                    new PrimaryDrawerItem()
                            .withName(R.string.title_activity_settings)
                            .withIcon(GoogleMaterial.Icon.gmd_settings)
                            .withIdentifier(6)
                            .withSelectable(false)
            );
        }
        mDrawer = mDrawerBuilder.build();
        //if you have many different types of DrawerItems you can magically pre-cache those items to get a better scroll performance
        //make sure to init the cache after the DrawerBuilder was created as this will first clear the cache to make sure no old elements are in
        RecyclerViewCacheUtil.getInstance().withCacheSize(2).init(mDrawer);

        //only set the active selection or active profile if we do not recreate the activity
        if (savedInstanceState == null) {
            // set the selection to the item with the identifier 1
            mDrawer.setSelection(1, false);

            //set the active profile
            //headerResult.setActiveProfile(profile);
        }
        //TODO:根据消息数量刷新Notification
        int notificationsNumber = Integer.parseInt(CarbonForumApplication.cacheSharedPreferences.getString("notificationsNumber", "0"));
        if(notificationsNumber>0){
            //添加消息通知
            mDrawer.updateBadge(4, new StringHolder(notificationsNumber + ""));
        }

    }
    //下拉刷新事件
    @Override
    public void onRefresh() {
        //if(!mSwipeRefreshLayout.isRefreshing()){
        loadTopic(1, false);
        //}
    }
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
    }
    @Override
    public void onBackPressed() {
        //handle the back press :D close the drawer first and if the drawer is closed close the activity
        if (mDrawer != null && mDrawer.isDrawerOpen()) {
            mDrawer.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }
    public class GetTopicsTask extends AsyncTask<Void, Void, List<Map<String,Object>>> {
        private int targetPage;
        private Boolean enableCache;
        private int positionStart;
        public GetTopicsTask(int targetPage, Boolean enableCache) {
            this.targetPage = targetPage;
            this.enableCache = enableCache;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            enableScrollListener = false;
            if(enableCache){
                topicList = JSONUtil.jsonObject2List(JSONUtil.jsonString2Object(
                        CarbonForumApplication.cacheSharedPreferences.getString("topicsCache", "{\"Status\":1, \"TopicsArray\":[]}"))
                        , "TopicsArray");
                if(topicList != null){
                    mAdapter.setData(topicList);
                    mAdapter.notifyDataSetChanged();
                }
            }
            mSwipeRefreshLayout.post(new Runnable(){
                @Override
                public void run(){
                    mSwipeRefreshLayout.setRefreshing(true);
                }
            });

            //Toast.makeText(IndexActivity.this, "Before AsyncTask", Toast.LENGTH_SHORT).show();

        }

        @Override
        protected void onPostExecute(List<Map<String, Object>> result) {
            super.onPostExecute(result);
            if(result!=null && !result.isEmpty()) {
                if (targetPage > 1) {
                    positionStart = topicList.size() - 1;
                    topicList.addAll(result);
                    mAdapter.setData(topicList);
                    //局部刷新，更好的性能
                    mAdapter.notifyItemRangeChanged(positionStart, mAdapter.getItemCount());
                } else {
                    topicList = result;
                    mAdapter.setData(topicList);
                    //全部刷新
                    mAdapter.notifyDataSetChanged();
                }
                //更新当前页数
                currentPage = targetPage;
            }else{
                Snackbar.make(mFloatingActionButton, R.string.network_error, Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
            //移除刷新控件
            mSwipeRefreshLayout.setRefreshing(false);
            enableScrollListener = true;
            //Toast.makeText(IndexActivity.this, "AsyncTask End", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected List<Map<String, Object>> doInBackground(Void... params) {
            List<Map<String,Object>> list;
            JSONObject jsonObject = HttpUtil.postRequest(IndexActivity.this, APIAddress.HOME_URL(targetPage), null, false, false);
            //Log.v("JSON", str);
            if(jsonObject != null){
                try {
                    totalPage = jsonObject.getInt("TotalPage");
                }catch(JSONException e){
                    e.printStackTrace();
                }
                if(targetPage == 1){
                    try {
                        SharedPreferences.Editor cacheEditor = CarbonForumApplication.cacheSharedPreferences.edit();
                        cacheEditor.putString("topicsCache", jsonObject.toString(0));
                        cacheEditor.apply();
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
            list = JSONUtil.jsonObject2List(jsonObject, "TopicsArray");
            //Log.v("List", list.toString());
            return list;
        }

    }
}


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;

import com.lincanbin.carbonforum.activity.AppCompatPreferenceActivity;

import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || DataSyncPreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("example_text"));
            bindPreferenceSummaryToValue(findPreference("example_list"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DataSyncPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_sync);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("sync_frequency"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;

import com.lincanbin.carbonforum.service.ReplyService;
import com.lincanbin.carbonforum.util.markdown.MarkdownProcessor;

public class ReplyActivity extends AppCompatActivity {
    Toolbar mToolbar;
    String mTopicID;
    String mPostID;
    String mPostFloor;
    String mUserName;
    String defaultContent;
    String contentHTML;
    EditText mContent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //取得启动该Activity的Intent对象
        Intent mIntent =getIntent();
        //取出Intent中附加的数据
        mTopicID = mIntent.getStringExtra("TopicID");
        mPostID = mIntent.getStringExtra("PostID");
        mPostFloor = mIntent.getStringExtra("PostFloor");
        mUserName = mIntent.getStringExtra("UserName");
        defaultContent = mIntent.getStringExtra("DefaultContent");
        setContentView(R.layout.activity_reply);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mContent = (EditText) findViewById(R.id.content);
        mContent.setText(defaultContent);
        //自动弹出键盘
        mContent.setFocusable(true);
        mContent.setFocusableInTouchMode(true);
        mContent.requestFocus();
        InputMethodManager mInputManager = (InputMethodManager)mContent.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        mInputManager.showSoftInput(mContent, 0);

        if (mToolbar != null) {
            //mToolbar.setTitle(getString(R.string.title_activity_reply));
            if(Integer.parseInt(mPostFloor) == 0){
                mToolbar.setTitle(getString(R.string.title_activity_reply));
            }else{
                mToolbar.setTitle(getString(R.string.action_reply_to) + " @" + mUserName);
            }
            setSupportActionBar(mToolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            ImageButton imageButton = (ImageButton) mToolbar.findViewById(R.id.reply_button);
            imageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mContent.getText().toString().length() > 0) {
                        MarkdownProcessor mMarkdownProcessor = new MarkdownProcessor();
                        int currentPostFloor = Integer.parseInt(mPostFloor);
                        if (currentPostFloor == 0) {
                            contentHTML = mMarkdownProcessor.markdown(mContent.getText().toString());
                        } else {
                            contentHTML = "<p>\n" + getString(R.string.action_reply_to) +
                                    " <a href=\"/t/" + mTopicID + "#Post" + mPostID + "\">#" + (currentPostFloor == -1 ? "0" : mPostFloor) + "</a> @" + mUserName + " :<br/>\n" +
                                    "</p><p>" + mMarkdownProcessor.markdown(mContent.getText().toString()) + "</p>";
                        }
                        Intent intent = new Intent(ReplyActivity.this, ReplyService.class);
                        intent.putExtra("TopicID", mTopicID);
                        intent.putExtra("Content", contentHTML);
                        startService(intent);
                        onBackPressed();
                    }else{
                        Snackbar.make(view, getString(R.string.content_empty), Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    }
                }
            });
        }
        //TODO: 根据草稿恢复现场
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.lincanbin.carbonforum.application.CarbonForumApplication;
import com.lincanbin.carbonforum.config.APIAddress;
import com.lincanbin.carbonforum.tools.VerificationCode;
import com.lincanbin.carbonforum.util.HttpUtil;
import com.lincanbin.carbonforum.util.JSONUtil;
import com.lincanbin.carbonforum.util.MD5Util;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.Manifest.permission.READ_CONTACTS;

/**
 * A register screen that offers register via email/password.
 */
public class RegisterActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;

    /**
     * Keep track of the register task to ensure we can cancel it if requested.
     */
    private UserRegisterTask mAuthTask = null;

    // UI references.
    private Toolbar mToolbar;
    private EditText mUsernameView;
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private EditText mVerificationCodeView;
    private ImageView mVerificationCodeImageView;
    private View mProgressView;
    private View mRegisterFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar != null) {
            mToolbar.setTitle(R.string.title_activity_register);
            setSupportActionBar(mToolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        mUsernameView = (EditText) findViewById(R.id.username);
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        populateAutoComplete();

        mVerificationCodeView = (EditText) findViewById(R.id.verification_code);
        mVerificationCodeView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE) {
                    attemptRegister();
                    return true;
                }
                return false;
            }
        });
        mVerificationCodeImageView = (ImageView)  findViewById(R.id.verification_code_img);
        mVerificationCodeImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshVerificationCode();
            }
        });
        refreshVerificationCode();
        
        mPasswordView = (EditText) findViewById(R.id.password);

        Button mRegisterButton = (Button) findViewById(R.id.register_button);
        mRegisterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptRegister();
            }
        });

        mRegisterFormView = findViewById(R.id.register_form);
        mProgressView = findViewById(R.id.register_progress);
    }
    
    private void refreshVerificationCode(){
        //接口回调的方法，完成验证码的异步读取与显示
        VerificationCode verificationCodeImage = new VerificationCode(this);
        verificationCodeImage.loadImage(new VerificationCode.ImageCallBack() {
            @Override
            public void getDrawable(Drawable drawable) {
                mVerificationCodeImageView.setImageDrawable(drawable);
            }
        });
    }
    private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }
        getLoaderManager().initLoader(0, null, this);
    }

    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(mEmailView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }

    /**
     * Attempts to sign in or register the account specified by the register form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual register attempt is made.
     */
    private void attemptRegister() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mUsernameView.setError(null);
        mEmailView.setError(null);
        mPasswordView.setError(null);
        mVerificationCodeView.setError(null);

        // Store values at the time of the register attempt.
        String username = mUsernameView.getText().toString();
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();
        String verification_code = mVerificationCodeView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid username address.
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        }
        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        // Check for a valid verification code.
        if (TextUtils.isEmpty(verification_code)) {
            mVerificationCodeView.setError(getString(R.string.error_field_required));
            focusView = mVerificationCodeView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt register and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user register attempt.
            showProgress(true);
            mAuthTask = new UserRegisterTask(username, email, password, verification_code);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() >= 5;
    }

    /**
     * Shows the progress UI and hides the register form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mRegisterFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mRegisterFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mRegisterFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mRegisterFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }


    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(RegisterActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }

    /**
     * Represents an asynchronous register/registration task used to authenticate
     * the user.
     */
    public class UserRegisterTask extends AsyncTask<Void, Void, JSONObject> {

        private final Map<String, String> parameter = new HashMap<>();

        UserRegisterTask(String username, String email, String password, String verification_code) {
            parameter.put("UserName", username);
            parameter.put("Email", email);
            parameter.put("Password", MD5Util.md5(password));
            parameter.put("VerifyCode", verification_code);
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            return HttpUtil.postRequest(RegisterActivity.this, APIAddress.REGISTER_URL, parameter, true, false);
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            mAuthTask = null;
            showProgress(false);
            if(result !=null) {
                try {
                    //Log.v("JSON", result.toString());
                    if (result.getInt("Status") == 1) {
                        //Log.v("JSON", result.toString());

                        SharedPreferences.Editor editor = CarbonForumApplication.userInfo.edit();
                        editor.putString("UserID", result.getString("UserID"));
                        editor.putString("UserExpirationTime", result.getString("UserExpirationTime"));
                        editor.putString("UserCode", result.getString("UserCode"));

                        JSONObject userInfo =  JSONUtil.jsonString2Object(result.getString("UserInfo"));
                        if(userInfo!=null){
                            editor.putString("UserName", userInfo.getString("UserName"));
                            editor.putString("UserRoleID", userInfo.getString("UserRoleID"));
                            editor.putString("UserMail", userInfo.getString("UserMail"));
                            editor.putString("UserIntro", userInfo.getString("UserIntro"));
                        }
                        editor.apply();
                        //发送广播刷新
                        Intent intent = new Intent();
                        intent.setAction("action.refreshDrawer");
                        LocalBroadcastManager.getInstance(RegisterActivity.this).sendBroadcast(intent);
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this, result.getString("ErrorMessage"), Toast.LENGTH_SHORT).show();
                        refreshVerificationCode();
                        switch(result.getInt("ErrorCode")){
                            case 104001:
                            case 104002:
                            case 104005:
                                mUsernameView.setError(result.getString("ErrorMessage"));
                                mUsernameView.requestFocus();
                                break;
                            case 104003:
                                mEmailView.setError(result.getString("ErrorMessage"));
                                mEmailView.requestFocus();
                                break;
                            case 104004:
                                mVerificationCodeView.setError(result.getString("ErrorMessage"));
                                mVerificationCodeView.requestFocus();
                                break;
                            default:
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else{
                Snackbar.make(mRegisterFormView, R.string.network_error, Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }

        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}



import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.lincanbin.carbonforum.application.CarbonForumApplication;
import com.lincanbin.carbonforum.config.APIAddress;
import com.lincanbin.carbonforum.tools.VerificationCode;
import com.lincanbin.carbonforum.util.HttpUtil;
import com.lincanbin.carbonforum.util.JSONUtil;
import com.lincanbin.carbonforum.util.MD5Util;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


/**
 * A login screen that offers login via username/password.
 */
public class LoginActivity extends AppCompatActivity {

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private Toolbar mToolbar;
    private EditText mUsernameView;
    private EditText mPasswordView;
    private EditText mVerificationCodeView;
    private ImageView mVerificationCodeImageView;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar != null) {
            mToolbar.setTitle(R.string.title_activity_login);
            setSupportActionBar(mToolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        // Set up the login form.
        mUsernameView = (EditText) findViewById(R.id.username);

        mPasswordView = (EditText) findViewById(R.id.password);

        mVerificationCodeView = (EditText) findViewById(R.id.verification_code);
        mVerificationCodeView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });
        mVerificationCodeImageView = (ImageView)  findViewById(R.id.verification_code_img);
        mVerificationCodeImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshVerificationCode();
            }
        });
        refreshVerificationCode();
        Button mUsernameSignInButton = (Button) findViewById(R.id.login_button);
        mUsernameSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    private void refreshVerificationCode(){
        //接口回调的方法，完成验证码的异步读取与显示
        VerificationCode verificationCodeImage = new VerificationCode(this);
        verificationCodeImage.loadImage(new VerificationCode.ImageCallBack() {
            @Override
            public void getDrawable(Drawable drawable) {
                mVerificationCodeImageView.setImageDrawable(drawable);
            }
        });
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid username, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mUsernameView.setError(null);
        mPasswordView.setError(null);
        mVerificationCodeView.setError(null);

        // Store values at the time of the login attempt.
        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();
        String verification_code = mVerificationCodeView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid username address.
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        /*
        } else if (!isUsernameValid(username)) {
            mUsernameView.setError(getString(R.string.error_invalid_username));
            focusView = mUsernameView;
            cancel = true;
         */
        }

        // Check for a valid verification code.
        if (TextUtils.isEmpty(verification_code)) {
            mVerificationCodeView.setError(getString(R.string.error_field_required));
            focusView = mVerificationCodeView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(username, password, verification_code);
            mAuthTask.execute((Void) null);
        }
    }
    /*
    private boolean isUsernameValid(String username) {
        return username.contains("@");
    }
    */

    private boolean isPasswordValid(String password) {
        return password.length() >= 3;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }


    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, JSONObject> {

        private final Map<String, String> parameter = new HashMap<>();

        UserLoginTask(String username, String password, String verification_code) {
            parameter.put("UserName", username);
            parameter.put("Password", MD5Util.md5(password));
            parameter.put("VerifyCode", verification_code);
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            return HttpUtil.postRequest(LoginActivity.this, APIAddress.LOGIN_URL,parameter, true, false);
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            mAuthTask = null;
            showProgress(false);
            if(result !=null) {
                try {
                    //Log.v("JSON", result.toString());
                    if (result.getInt("Status") == 1) {
                        //Log.v("JSON", result.toString());

                        SharedPreferences.Editor editor = CarbonForumApplication.userInfo.edit();
                        editor.putString("UserID", result.getString("UserID"));
                        editor.putString("UserExpirationTime", result.getString("UserExpirationTime"));
                        editor.putString("UserCode", result.getString("UserCode"));

                        JSONObject userInfo =  JSONUtil.jsonString2Object(result.getString("UserInfo"));
                        if(userInfo!=null){
                            editor.putString("UserName", userInfo.getString("UserName"));
                            editor.putString("UserRoleID", userInfo.getString("UserRoleID"));
                            editor.putString("UserMail", userInfo.getString("UserMail"));
                            editor.putString("UserIntro", userInfo.getString("UserIntro"));
                        }
                        editor.apply();
                        //发送广播刷新
                        Intent intent = new Intent();
                        intent.setAction("action.refreshDrawer");
                        LocalBroadcastManager.getInstance(LoginActivity.this).sendBroadcast(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, result.getString("ErrorMessage"), Toast.LENGTH_SHORT).show();
                        refreshVerificationCode();
                        switch(result.getInt("ErrorCode")){
                            case 101001:
                            case 101003:
                                mUsernameView.setError(result.getString("ErrorMessage"));
                                mUsernameView.requestFocus();
                                break;
                            case 101004:
                                mPasswordView.setError(result.getString("ErrorMessage"));
                                mPasswordView.requestFocus();
                                break;
                            case 101002:
                                mVerificationCodeView.setError(result.getString("ErrorMessage"));
                                mVerificationCodeView.requestFocus();
                                break;
                            default:
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else{
                Snackbar.make(mLoginFormView, R.string.network_error, Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }

        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.lincanbin.carbonforum.adapter.PostAdapter;
import com.lincanbin.carbonforum.application.CarbonForumApplication;
import com.lincanbin.carbonforum.config.APIAddress;
import com.lincanbin.carbonforum.util.HttpUtil;
import com.lincanbin.carbonforum.util.JSONUtil;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationsActivity extends AppCompatActivity{

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        ImageButton imageButton = (ImageButton) toolbar.findViewById(R.id.notifications_settings_button);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(NotificationsActivity.this, SettingsActivity.class);
                intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.NotificationPreferenceFragment.class.getName());
                intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
                startActivity(intent);
            }
        });
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return  getString(R.string.notifications_mentioned_me);
                case 1:
                    return getString(R.string.notifications_replied_to_me);
            }
            return null;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment{
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "notifications_type";
        private static View rootView;
        private static SwipeRefreshLayout mSwipeRefreshLayout;
        private static RecyclerView mRecyclerView;
        private static PostAdapter mAdapter;
        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            final int type = getArguments().getInt(ARG_SECTION_NUMBER);
            rootView = inflater.inflate(R.layout.fragment_notifications, container, false);
            mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.activity_notifications_swipe_refresh_layout);
            mSwipeRefreshLayout.setColorSchemeResources(
                    R.color.material_light_blue_700,
                    R.color.material_red_700,
                    R.color.material_orange_700,
                    R.color.material_light_green_700
            );
            //RecyclerView
            mRecyclerView = (RecyclerView) rootView.findViewById(R.id.notifications_list);
            mRecyclerView.setHasFixedSize(true);
            LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    //TODO 加载更多提醒
                }
            });
            mRecyclerView.setLayoutManager(layoutManager);
            mRecyclerView.setItemAnimator(new DefaultItemAnimator());
            mAdapter = new PostAdapter(getActivity(), true);
            mAdapter.setData(new ArrayList<Map<String, Object>>());
            mRecyclerView.setAdapter(mAdapter);
            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    new GetNotificationsTask(type, false, mSwipeRefreshLayout, mRecyclerView, mAdapter, 1).execute();
                }
            });
            new GetNotificationsTask(type, true, mSwipeRefreshLayout, mRecyclerView, mAdapter, 1).execute();
            return rootView;
        }

        public class GetNotificationsTask extends AsyncTask<Void, Void, JSONObject> {
            private int targetPage;
            private int type;
            private String keyName;
            private Boolean loadFromCache;
            private SwipeRefreshLayout mSwipeRefreshLayout;
            private RecyclerView mRecyclerView;
            private PostAdapter mAdapter;
            public GetNotificationsTask(int type,
                                        Boolean loadFromCache,
                                        SwipeRefreshLayout mSwipeRefreshLayout,
                                        RecyclerView mRecyclerView,
                                        PostAdapter mAdapter,
                                        int targetPage) {
                this.targetPage = targetPage;
                this.type = type;
                this.keyName = type == 1 ? "ReplyArray" : "MentionArray";
                this.loadFromCache = loadFromCache;
                this.mSwipeRefreshLayout = mSwipeRefreshLayout;
                this.mRecyclerView = mRecyclerView;
                this.mAdapter = mAdapter;
            }
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                if(!loadFromCache) {
                    mSwipeRefreshLayout.post(new Runnable() {
                        @Override
                        public void run() {
                            mSwipeRefreshLayout.setRefreshing(true);
                        }
                    });
                }
            }

            @Override
            protected void onPostExecute(JSONObject jsonObject) {
                super.onPostExecute(jsonObject);
                int status = 0;
                if(loadFromCache){
                    status = 1;
                }
                //先保存缓存
                if(jsonObject != null && !loadFromCache){
                    try {
                        status = jsonObject.getInt("Status");
                        SharedPreferences.Editor cacheEditor = CarbonForumApplication.cacheSharedPreferences.edit();
                        //cacheEditor.putString("notifications" + keyName + "Cache", jsonObject.toString(0));
                        cacheEditor.putString("notificationsMentionArrayCache", jsonObject.toString(0));
                        cacheEditor.putString("notificationsReplyArrayCache", jsonObject.toString(0));
                        cacheEditor.apply();
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
                //更新界面
                List<Map<String, Object>> list;
                list = JSONUtil.jsonObject2List(jsonObject, keyName);
                //防止异步任务未完成时，用户按下返回，Fragment被GC，造成NullPointer
                if(mRecyclerView != null && mSwipeRefreshLayout !=null && mAdapter != null && rootView != null && getActivity() != null) {
                    mSwipeRefreshLayout.setRefreshing(false);
                    Log.d("Status : ", keyName + String.valueOf(status));
                    if (status == 1) {
                        if(list != null && !list.isEmpty()){
                            Log.d("Action : ", keyName + " SetData");
                            mAdapter.setData(list);
                            mAdapter.notifyDataSetChanged();
                        }else{
                            //新注册用户，网络正常但是当前无任何通知，做个提示
                            Snackbar.make(rootView, R.string.empty_notification, Snackbar.LENGTH_LONG).setAction("Action", null).show();
                        }
                    } else {
                        Snackbar.make(rootView, R.string.network_error, Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    }
                }
                //在提及我的的Tab中，从缓存中加载一次后，再从网络上更新一次
                if(type == 1 && loadFromCache){
                    new GetNotificationsTask(type, false, mSwipeRefreshLayout, mRecyclerView, mAdapter, 1).execute();
                }
            }

            @Override
            protected JSONObject doInBackground(Void... params) {
                if(loadFromCache){
                    return JSONUtil.jsonString2Object(
                            CarbonForumApplication.cacheSharedPreferences.
                                    getString("notifications" + keyName + "Cache", "{\"Status\":1, \"" + keyName + "\":[]}")
                    );
                }else {
                    return HttpUtil.postRequest(getActivity(), APIAddress.NOTIFICATIONS_URL, new HashMap<String, String>(), false, true);
                }
            }

        }
    }

}

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;

import com.lincanbin.carbonforum.config.APIAddress;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by 灿斌 on 5/18/2015.
 */
public class VerificationCode {
    Context context;
    public VerificationCode(Context context){this.context = context;}

    public void loadImage(final ImageCallBack callBack){

        final Handler handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Drawable drawable = (Drawable) msg.obj;
                callBack.getDrawable(drawable);
            }
        };

        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    URL localURL = new URL(APIAddress.VERIFICATION_CODE);
                    URLConnection connection = localURL.openConnection();
                    HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
                    InputStream inputStream = null;
                    inputStream = httpURLConnection.getInputStream();
                    //获取Cookie
                    String headerName=null;
                    for (int i=1; (headerName = connection.getHeaderFieldKey(i))!=null; i++) {
                        if (headerName.equals("Set-Cookie")) {
                            String cookie = connection.getHeaderField(i);
                            //cookie = cookie.substring(0, cookie.indexOf(";"));
                            //String cookieName = cookie.substring(0, cookie.indexOf("="));
                            //String cookieValue = cookie.substring(cookie.indexOf("=") + 1, cookie.length());
                            //将Cookie保存起来
                            SharedPreferences mySharedPreferences= context.getSharedPreferences("Session",
                                    Activity.MODE_PRIVATE);
                            SharedPreferences.Editor editor = mySharedPreferences.edit();
                            editor.putString("Cookie", cookie);
                            editor.apply();
                        }
                    }
                    Drawable drawable = Drawable.createFromStream(inputStream, "");
                    Message message = Message.obtain();
                    message.obj = drawable;
                    handler.sendMessage(message);

                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    public interface ImageCallBack{
        void getDrawable(Drawable drawable);
    }
}


import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.lincanbin.carbonforum.R;
import com.lincanbin.carbonforum.TopicActivity;
import com.lincanbin.carbonforum.config.APIAddress;
import com.lincanbin.carbonforum.util.TimeUtil;

import java.util.List;
import java.util.Map;

/**
 * Created by 灿斌 on 5/14/2015.
 */
public class TopicAdapter extends RecyclerView.Adapter{
    private Context context;
    private LayoutInflater layoutInflater;
    public interface OnRecyclerViewListener {
        void onItemClick(int position);
        boolean onItemLongClick(int position);
    }

    private OnRecyclerViewListener onRecyclerViewListener;

    public void setOnRecyclerViewListener(OnRecyclerViewListener onRecyclerViewListener) {
        this.onRecyclerViewListener = onRecyclerViewListener;
    }

    private static final String TAG = TopicAdapter.class.getSimpleName();
    private List<Map<String,Object>> list;
    public TopicAdapter(Context context){
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
    }
    public void setData(List<Map<String,Object>> list) {
        this.list = list;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {

        View view = layoutInflater.inflate(R.layout.item_topic_list, null);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(lp);
        return new topicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        final topicViewHolder holder = (topicViewHolder) viewHolder;
        holder.position = i;
        Map<String,Object> topic = list.get(i);
        holder.Title.setText(Html.fromHtml(topic.get("Topic").toString()).toString());
        holder.Description.setText(topic.get("UserName").toString() + " · " + topic.get("LastName").toString());
        holder.Time.setText(TimeUtil.formatTime(context, Long.parseLong(topic.get("LastTime").toString())));
        Glide.with(context).load(APIAddress.MIDDLE_AVATAR_URL(topic.get("UserID").toString(), "middle")).into(holder.Avatar);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class topicViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener
    {
        public View rootView;
        ImageView Avatar;
        TextView Title;
        TextView Description;
        TextView Time;
        public int position;

        public topicViewHolder(View itemView) {
            super(itemView);

            Title = (TextView) itemView.findViewById(R.id.title);
            Description = (TextView) itemView.findViewById(R.id.description);
            Time = (TextView) itemView.findViewById(R.id.time);
            Avatar = (ImageView)itemView.findViewById(R.id.avatar);

            rootView = itemView.findViewById(R.id.topic_item);
            //rootView.setClickable(true);
            rootView.setOnClickListener(this);
            rootView.setOnLongClickListener(this);
        }
        @Override
        //点击事件
        public void onClick(View v) {
            //Toast.makeText(context, "onItemClick" + list.get(position).get("Topic").toString(), Toast.LENGTH_SHORT).show();
            //v.getBackground().setColorFilter(Color.parseColor("#90A4AE"), PorterDuff.Mode.DARKEN);
            Intent intent = new Intent(context, TopicActivity.class);
            intent.putExtra("Topic", list.get(position).get("Topic").toString());
            intent.putExtra("TopicID", list.get(position).get("ID").toString());
            intent.putExtra("TargetPage", "1");
            context.startActivity(intent);
            //if (null != onRecyclerViewListener) {
                //onRecyclerViewListener.onItemClick(position);
            //}
        }

        @Override
        //长按事件
        public boolean onLongClick(View v) {
            if(null != onRecyclerViewListener){
                return onRecyclerViewListener.onItemLongClick(position);
            }
            return false;
        }
    }
}


import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.lincanbin.carbonforum.R;
import com.lincanbin.carbonforum.ReplyActivity;
import com.lincanbin.carbonforum.TopicActivity;
import com.lincanbin.carbonforum.application.CarbonForumApplication;
import com.lincanbin.carbonforum.config.APIAddress;
import com.lincanbin.carbonforum.util.TimeUtil;
import com.lincanbin.carbonforum.view.CarbonWebView;

import java.util.List;
import java.util.Map;

/**
 * Created by 灿斌 on 10/13/2015.
 */
public class PostAdapter extends RecyclerView.Adapter{
    private Context context;
    private Boolean isNotification;
    private LayoutInflater layoutInflater;
    public interface OnRecyclerViewListener {
        void onItemClick(int position);
        boolean onItemLongClick(int position);
    }

    private OnRecyclerViewListener onRecyclerViewListener;

    public void setOnRecyclerViewListener(OnRecyclerViewListener onRecyclerViewListener) {
        this.onRecyclerViewListener = onRecyclerViewListener;
    }

    private static final String TAG = PostAdapter.class.getSimpleName();
    private List<Map<String,Object>> list;
    public PostAdapter(Context context, Boolean isNotification){
        this.context = context;
        this.isNotification = isNotification;
        layoutInflater = LayoutInflater.from(context);
    }
    public void setData(List<Map<String,Object>> list) {
        this.list = list;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {

        View view = layoutInflater.inflate(R.layout.item_post_list, null);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(lp);
        return new postViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        final postViewHolder holder = (postViewHolder) viewHolder;
        holder.position = i;
        final Map<String,Object> post = list.get(i);
        holder.UserName.setText(post.get("UserName").toString());
        holder.Time.setText(TimeUtil.formatTime(context, Long.parseLong(post.get("PostTime").toString())));
        if(!isNotification && !post.get("PostFloor").toString().equals("0"))
            holder.PostFloor.setText("#" + post.get("PostFloor").toString());
        String contentHTML = "<style>" +
                "a, a:link, a:visited, a:active {" +
                "   color: #555555;" +
                "   text-decoration: none;" +
                "   word-wrap: break-word;" +
                "}" +
                "a:hover {" +
                "   color: #7aa1b0;" +
                "}" +
                "p, h3{" +
                "   color:#616161;" +
                "}" +
                "img, video{" +
                "   display: inline;" +
                "   height: auto;" +
                "   max-width: 100%;" +
                "}" +
                "</style>";
        if(isNotification){
            contentHTML += "<h3>" + post.get("Subject").toString() + "</h3>";
        }
        //String uploadDomain = APIAddress.WEBSITE_PATH.length() > 0 ? APIAddress.DOMAIN_NAME.replace(APIAddress.WEBSITE_PATH, "") : APIAddress.DOMAIN_NAME;
        //contentHTML += post.get("Content").toString().replace("=\"/", "=\"" + uploadDomain + "/");
        contentHTML += post.get("Content").toString();
        //Log.v("Post"+ post.get("ID").toString(), contentHTML);
        holder.Content.loadDataWithBaseURL(APIAddress.MOBILE_DOMAIN_NAME, contentHTML, "text/html", "utf-8", null);
        Glide.with(context).load(APIAddress.MIDDLE_AVATAR_URL(post.get("UserID").toString(), "middle")).into(holder.Avatar);
        holder.ReplyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, ReplyActivity.class);
                intent.putExtra("TopicID", post.get("TopicID").toString());
                intent.putExtra("PostID", post.get("ID").toString());
                intent.putExtra("PostFloor", post.get("PostFloor").toString());
                intent.putExtra("UserName", post.get("UserName").toString());
                intent.putExtra("DefaultContent", "");
                context.startActivity(intent);
            }
        });
        if(!CarbonForumApplication.isLoggedIn()){
            holder.ReplyButton.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class postViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener
    {
        public View cardView;
        View rootView;
        ImageView Avatar;
        TextView Time;
        TextView UserName;
        TextView PostFloor;
        ImageView ReplyButton;
        CarbonWebView Content;
        public int position;

        public postViewHolder(View itemView) {
            super(itemView);

            UserName = (TextView) itemView.findViewById(R.id.username);
            PostFloor = (TextView) itemView.findViewById(R.id.floor);
            ReplyButton = (ImageView)itemView.findViewById(R.id.reply_button);
            Content = (CarbonWebView) itemView.findViewById(R.id.content);
            if(Build.VERSION.SDK_INT <= 19) {
                // http://stackoverflow.com/questions/15133132/android-webview-doesnt-display-web-page-in-some-cases
                Content.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            } else {
                //Content.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                //Content.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                // 修复显示长帖时申请不到内存的bug
                // http://stackoverflow.com/questions/18471194/webview-in-scrollview-view-too-large-to-fit-into-drawing-cache-how-to-rewor
                Content.setLayerType(View.LAYER_TYPE_NONE, null);
            }
            // http://stackoverflow.com/questions/5003156/android-webview-style-background-colortransparent-ignored-on-android-2-2
            Content.setBackgroundColor(Color.TRANSPARENT);
            Content.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);//优先使用缓存
            // http://stackoverflow.com/questions/3099344/can-androids-webview-automatically-resize-huge-images/12327010#12327010
            Content.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);//自动缩放图片
            // Use WideViewport and Zoom out if there is no viewport defined
            //Content.getSettings().setUseWideViewPort(true);
            /*
            // Enable remote debugging via chrome://inspect
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                WebView.setWebContentsDebuggingEnabled(true);
            }
            */
            Time = (TextView) itemView.findViewById(R.id.time);
            Avatar = (ImageView)itemView.findViewById(R.id.avatar);
            cardView = itemView.findViewById(R.id.post_card_item);
            rootView = itemView.findViewById(R.id.post_item);
            rootView.setOnClickListener(this);
            rootView.setOnLongClickListener(this);
        }


        @Override
        //点击事件
        public void onClick(View v) {
            //Toast.makeText(context, "onItemClick", Toast.LENGTH_SHORT).show();
            if(isNotification) {
                Intent intent = new Intent(context, TopicActivity.class);
                intent.putExtra("Topic", list.get(position).get("Subject").toString());
                intent.putExtra("TopicID", list.get(position).get("TopicID").toString());
                intent.putExtra("TargetPage", "1");
                context.startActivity(intent);
                //if (null != onRecyclerViewListener) {
                //    onRecyclerViewListener.onItemClick(position);
                //}
            }
        }

        @Override
        //长按事件
        public boolean onLongClick(View v) {
            //ReplyButton.callOnClick();
            if(null != onRecyclerViewListener){
                return onRecyclerViewListener.onItemLongClick(position);
            }
            return false;
        }
    }
}


import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.lincanbin.carbonforum.R;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader;
import com.mikepenz.materialdrawer.util.DrawerImageLoader;
import com.mikepenz.materialdrawer.util.DrawerUIUtils;

/**
 * Created by 灿斌 on 10/12/2015.
 */
public class CarbonForumApplication extends Application {

    public static SharedPreferences userInfo;
    public static SharedPreferences cacheSharedPreferences;

    public static Boolean isLoggedIn(){
        return Integer.parseInt(userInfo.getString("UserID", "0")) > 0;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        userInfo = getApplicationContext().getSharedPreferences("UserInfo", Activity.MODE_PRIVATE);
        //获取缓存
        cacheSharedPreferences = getSharedPreferences("MainCache", Activity.MODE_PRIVATE);
        /*
        //initialize and create the image loader logic
        DrawerImageLoader.init(new AbstractDrawerImageLoader() {
            @Override
            public void set(ImageView imageView, Uri uri, Drawable placeholder) {
                Picasso.with(imageView.getContext()).load(uri).placeholder(placeholder).into(imageView);
            }
            @Override
            public void cancel(ImageView imageView) {
                Picasso.with(imageView.getContext()).cancelRequest(imageView);
            }
        });
        */

        //initialize and create the image loader logic
        DrawerImageLoader.init(new AbstractDrawerImageLoader() {
            @Override
            public void set(ImageView imageView, Uri uri, Drawable placeholder) {
                Glide.with(imageView.getContext()).load(uri).placeholder(placeholder).into(imageView);
            }

            @Override
            public void cancel(ImageView imageView) {
                Glide.clear(imageView);
            }

            @Override
            public Drawable placeholder(Context ctx, String tag) {
                //define different placeholders for different imageView targets
                //default tags are accessible via the DrawerImageLoader.Tags
                //custom ones can be checked via string. see the CustomUrlBasePrimaryDrawerItem LINE 111
                if (DrawerImageLoader.Tags.PROFILE.name().equals(tag)) {
                    return DrawerUIUtils.getPlaceHolder(ctx);
                } else if (DrawerImageLoader.Tags.ACCOUNT_HEADER.name().equals(tag)) {
                    return new IconicsDrawable(ctx).iconText(" ").backgroundColorRes(com.mikepenz.materialdrawer.R.color.primary).sizeDp(56);
                } else if ("customUrlItem".equals(tag)) {
                    return new IconicsDrawable(ctx).iconText(" ").backgroundColorRes(R.color.md_blue_500).sizeDp(56);
                }

                //we use the default one for
                //DrawerImageLoader.Tags.PROFILE_DRAWER_ITEM.name()

                return super.placeholder(ctx, tag);
            }
        });
    }
}

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.lincanbin.carbonforum.R;
import com.lincanbin.carbonforum.config.APIAddress;
import com.lincanbin.carbonforum.util.HttpUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ReplyService extends IntentService {
    public String mTopicID = "0";
    public String mContent = "";

    public ReplyService() {
        super("ReplyService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            mTopicID = intent.getStringExtra("TopicID");
            mContent = intent.getStringExtra("Content");
            reply();
        }
    }

    private void reply() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        final Map<String, String> parameter = new HashMap<>();
        parameter.put("TopicID", mTopicID);
        parameter.put("Content", mContent);
        //显示“回复中”提示
        String shortContent = mContent.replaceAll("<!--.*?-->", "").replaceAll("<[^>]+>", "");//移除HTML标签
        final Notification.Builder builder = new Notification.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.replying))
                .setContentText(shortContent.subSequence(0, shortContent.length()))
                .setOngoing(true);
        if (Build.VERSION.SDK_INT >= 16) {
            mNotificationManager.notify(102001, builder.build());
        }else{
            mNotificationManager.notify(102001, builder.getNotification());
        }
        final JSONObject jsonObject = HttpUtil.postRequest(getApplicationContext(), APIAddress.REPLY_URL, parameter, false, true);
        // 移除“回复中”通知
        mNotificationManager.cancel(102001);
        try {
            if(jsonObject != null && jsonObject.getInt("Status") == 1) {
                //回帖成功，并发送广播告知成功
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), getString(R.string.reply_success), Toast.LENGTH_SHORT).show();
                    }
                });
                //发送广播刷新帖子（如果还在看那个帖子的话）
                Intent intent = new Intent();
                intent.putExtra("TargetPage", jsonObject.getInt("Page"));
                intent.setAction("action.refreshTopic");
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

            } else {
                //回帖不成功，Toast，并添加重发的通知栏通知
                PendingIntent mPendingIntent = PendingIntent.getService(
                        getApplicationContext(),
                        0,
                        new Intent(getApplicationContext(), ReplyService.class).putExtra("TopicID", mTopicID).putExtra("Content", mContent),
                        0
                );
                final Notification.Builder failBuilder = new Notification.Builder(getApplicationContext())
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(getString(R.string.resend_reply))
                        .setContentText(shortContent.subSequence(0, shortContent.length()))
                        .setContentIntent(mPendingIntent)
                        .setAutoCancel(true);
                if (Build.VERSION.SDK_INT >= 16) {
                    mNotificationManager.notify(102003, failBuilder.build());
                }else{
                    mNotificationManager.notify(102003, failBuilder.getNotification());
                }
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {

                        if(jsonObject != null) {
                            try {
                                Toast.makeText(getApplicationContext(), jsonObject.getString("ErrorMessage"), Toast.LENGTH_SHORT).show();
                            }catch(JSONException e){
                                e.printStackTrace();
                            }
                        }else{
                            Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                        }

                    }
                });
            }
        }catch(JSONException e){
            e.printStackTrace();
        }
    }


}

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.lincanbin.carbonforum.R;
import com.lincanbin.carbonforum.TopicActivity;
import com.lincanbin.carbonforum.config.APIAddress;
import com.lincanbin.carbonforum.util.HttpUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class NewService extends IntentService {
    public String mTitle = "";
    public String mTag = "";
    public String mContent = "";

    public NewService() {
        super("NewService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            mTitle = intent.getStringExtra("Title");
            mTag = intent.getStringExtra("Tag");
            mContent = intent.getStringExtra("Content");
            newTopic();
        }
    }

    private void newTopic(){
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        final Map<String, String> parameter = new HashMap<>();
        String[] TagsArray= mTag.replace("，",",").split(",");
        parameter.put("Title", mTitle);
        for(String mTagItem:TagsArray) {
            parameter.put("Tag[]#" + mTagItem, mTagItem);
        }
        parameter.put("Content", mContent);

        //显示“发送中”提示
        String shortContent = mContent.replaceAll("<!--.*?-->", "").replaceAll("<[^>]+>", "");//移除HTML标签
        final Notification.Builder builder = new Notification.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.sending))
                .setContentText(shortContent.subSequence(0, shortContent.length()))
                .setOngoing(true);
        if (Build.VERSION.SDK_INT >= 16) {
            mNotificationManager.notify(102001, builder.build());
        }else{
            mNotificationManager.notify(102001, builder.getNotification());
        }
        final JSONObject jsonObject = HttpUtil.postRequest(getApplicationContext(), APIAddress.NEW_URL, parameter, false, true);
        // 移除“发送中”通知
        mNotificationManager.cancel(102001);
        try {
            if(jsonObject != null && jsonObject.getInt("Status") == 1) {
                //发帖成功，并跳转Activity
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), getString(R.string.send_success), Toast.LENGTH_SHORT).show();
                    }
                });
                //跳转Activity
                Intent intent = new Intent(getApplicationContext(), TopicActivity.class);
                intent.putExtra("Topic", mTitle );
                intent.putExtra("TopicID", jsonObject.getString("TopicID"));
                intent.putExtra("TargetPage", "1");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

            } else {
                //回帖不成功，Toast，并添加重发的通知栏通知
                PendingIntent mPendingIntent = PendingIntent.getService(
                        getApplicationContext(),
                        0,
                        new Intent(getApplicationContext(), NewService.class)
                                .putExtra("Title", mTitle)
                                .putExtra("Tag", mTag)
                                .putExtra("Content", mContent),
                        0
                );
                final Notification.Builder failBuilder = new Notification.Builder(getApplicationContext())
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(getString(R.string.resend_topic))
                        .setContentText(shortContent.subSequence(0, shortContent.length()))
                        .setContentIntent(mPendingIntent)
                        .setAutoCancel(true);
                if (Build.VERSION.SDK_INT >= 16) {
                    mNotificationManager.notify(102003, failBuilder.build());
                }else{
                    mNotificationManager.notify(102003, failBuilder.getNotification());
                }
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(jsonObject != null) {
                            try {
                                Toast.makeText(getApplicationContext(), jsonObject.getString("ErrorMessage"), Toast.LENGTH_SHORT).show();
                            }catch(JSONException e){
                                e.printStackTrace();
                            }
                        }else{
                            Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                        }

                    }
                });
            }
        }catch(JSONException e){
            e.printStackTrace();
        }
    }
}


import android.app.Activity;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;

import com.lincanbin.carbonforum.NotificationsActivity;
import com.lincanbin.carbonforum.R;
import com.lincanbin.carbonforum.application.CarbonForumApplication;
import com.lincanbin.carbonforum.config.APIAddress;
import com.lincanbin.carbonforum.util.HttpUtil;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class PushService extends IntentService {
    public PushService() {
        super("PushService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(CarbonForumApplication.isLoggedIn()) {
            getNotification();
        }
    }
    private void getNotification(){
        int sleepTime = 3000;
        final Map<String, String> parameter = new HashMap<>();
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences cacheSharedPreferences = getSharedPreferences("MainCache", Activity.MODE_PRIVATE);
        int notificationsNumber = Integer.parseInt(cacheSharedPreferences.getString("notificationsNumber", "0"));

        JSONObject jsonObject = HttpUtil.postRequest(getApplicationContext(), APIAddress.PUSH_SERVICE_URL, parameter, false, true);
        try {
            if(jsonObject != null && jsonObject.getInt("Status") == 1){
                int newMessageNumber = jsonObject.getInt("NewMessage");
                //请求成功，延长请求间隔
                if(newMessageNumber > 0){
                    //消息数量大于0，发送通知栏消息
                    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    //跳转到通知页的intent
                    PendingIntent mPendingIntent = PendingIntent.getActivity(
                            getApplicationContext(),
                            0,
                            new Intent(getApplicationContext(), NotificationsActivity.class),
                            0
                    );
                    final Notification.Builder builder = new Notification.Builder(getApplicationContext())
                            .setSmallIcon(R.drawable.ic_launcher)
                            .setContentTitle(getString(R.string.new_message).replace("{{NewMessage}}", String.valueOf(newMessageNumber)))
                            .setContentText(getString(R.string.app_name))
                            .setContentIntent(mPendingIntent)
                            .setAutoCancel(true);
                    //有新通知的话才振动与响铃
                    if(newMessageNumber != notificationsNumber){
                        //设置振动
                        if(mSharedPreferences.getBoolean("notifications_new_message_vibrate", true)){
                            builder.setLights(Color.BLUE, 500, 500);
                            long[] pattern = {500,500,500,500,500};
                            builder.setVibrate(pattern);
                        }
                        //设置铃声
                        String ringtoneURI = mSharedPreferences.getString("notifications_new_message_ringtone", "content://settings/system/notification_sound");
                        if(!ringtoneURI.isEmpty()){
                            //Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                            Uri alarmSound =  Uri.parse(ringtoneURI);
                            builder.setSound(alarmSound);
                        }
                    }
                    mNotificationManager.cancel(105);
                    if (Build.VERSION.SDK_INT >= 16) {
                        mNotificationManager.notify(105, builder.build());
                    }else{
                        mNotificationManager.notify(105, builder.getNotification());
                    }
                    //请求成功，延长请求间隔
                    sleepTime = 30000;
                }
                //发送广播刷新Drawer
                Intent intent = new Intent();
                intent.setAction("action.refreshDrawer");
                sendBroadcast(intent);
                //保存当前消息数，每次判断消息数量与之前不一致才发送通知。
                try {
                    SharedPreferences.Editor cacheEditor = cacheSharedPreferences.edit();
                    cacheEditor.putString("notificationsNumber", Integer.toString(newMessageNumber));
                    cacheEditor.apply();
                }catch(Exception e){
                    e.printStackTrace();
                }
            }else{
                //请求失败，延长请求间隔
                sleepTime = 30000;
            }

            Thread.sleep(sleepTime);
        }catch(Exception e){
            e.printStackTrace();
        }
        boolean notifications_new_message = mSharedPreferences.getBoolean("notifications_new_message", false);
        if(notifications_new_message) {
            startService(new Intent(this, PushService.class));
        }else {
            stopSelf();
        }
    }
}


import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatCheckedTextView;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.lincanbin.carbonforum.R;

/**
 * A {@link android.preference.PreferenceActivity} which implements and proxies the necessary calls
 * to be used with AppCompat.
 */
public abstract class AppCompatPreferenceActivity extends PreferenceActivity {

    private AppCompatDelegate mDelegate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        //添加toolBar
        ViewGroup rootView = (ViewGroup) findViewById(android.R.id.content);
        View content = rootView.getChildAt(0);
        LinearLayout toolbarLayout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.toolbar_settings, null);
        rootView.removeAllViews();
        toolbarLayout.addView(content);
        rootView.addView(toolbarLayout);
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        //mToolbar.setTitle(R.string.title_activity_settings);
        setSupportActionBar(mToolbar);

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                //NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        // Allow super to try and create a view first
        final View result = super.onCreateView(name, context, attrs);
        if (result != null) {
            return result;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // If we're running pre-L, we need to 'inject' our tint aware Views in place of the
            // standard framework versions
            switch (name) {
                case "EditText":
                    return new AppCompatEditText(this, attrs);
                case "Spinner":
                    return new AppCompatSpinner(this, attrs);
                case "CheckBox":
                    return new AppCompatCheckBox(this, attrs);
                case "RadioButton":
                    return new AppCompatRadioButton(this, attrs);
                case "CheckedTextView":
                    return new AppCompatCheckedTextView(this, attrs);
            }
        }

        return null;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
    }

    public ActionBar getSupportActionBar() {
        return getDelegate().getSupportActionBar();
    }

    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        getDelegate().setSupportActionBar(toolbar);
    }

    @Override
    public MenuInflater getMenuInflater() {
        return getDelegate().getMenuInflater();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
    }

    @Override
    public void setContentView(View view) {
        getDelegate().setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().setContentView(view, params);
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().addContentView(view, params);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        getDelegate().setTitle(title);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getDelegate().onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStop() {
        super.onStop();
        getDelegate().onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getDelegate().onDestroy();
    }

    public void invalidateOptionsMenu() {
        getDelegate().invalidateOptionsMenu();
    }

    private AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }
}


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.lincanbin.carbonforum.LoginActivity;
import com.lincanbin.carbonforum.application.CarbonForumApplication;
import com.lincanbin.carbonforum.config.APIAddress;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;

public class HttpUtil {
    private static String charset = "utf-8";
    private Integer connectTimeout = null;
    private Integer socketTimeout = null;
    private String proxyHost = null;
    private Integer proxyPort = null;

    // get方法访问服务器，返回json对象
    public static JSONObject getRequest(Context context, String url, Map<String, String> parameterMap, Boolean enableSession, Boolean loginRequired) {
        try {
            Log.d("GET URL : ", url);
            String parameterString = buildParameterString(parameterMap, loginRequired);
            Log.d("GET parameter", parameterString);

            URL localURL = new URL(url + "?" + parameterString);

            URLConnection connection = localURL.openConnection();

            HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
            httpURLConnection.setRequestProperty("Accept-Charset", charset);
            httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            String cookie = getCookie(context);
            if(enableSession && cookie != null){
                httpURLConnection.setRequestProperty("Cookie", cookie);
            }
            InputStream inputStream = null;
            InputStreamReader inputStreamReader = null;
            BufferedReader reader = null;
            StringBuilder resultBuffer = new StringBuilder();
            String tempLine = null;

            switch (httpURLConnection.getResponseCode()){
                case 200:
                case 301:
                case 302:
                case 404:
                    break;
                case 403:
                    Log.d("Configuration error", "API_KEY or API_SECRET or system time error.");
                    return null;
                case 401:
                    context.getSharedPreferences("UserInfo",Activity.MODE_PRIVATE).edit().clear().apply();
                    Intent intent = new Intent(context, LoginActivity.class);
                    context.startActivity(intent);
                    break;
                case 500:
                    Log.d("Get Result","Code 500");
                    return null;
                default:
                    throw new Exception("HTTP Request is not success, Response code is " + httpURLConnection.getResponseCode());
            }
            if(enableSession) {
                saveCookie(context, httpURLConnection);
            }
            try {
                inputStream = httpURLConnection.getInputStream();
                inputStreamReader = new InputStreamReader(inputStream);
                reader = new BufferedReader(inputStreamReader);

                while ((tempLine = reader.readLine()) != null) {
                    resultBuffer.append(tempLine);
                }

                String getResult = resultBuffer.toString();
                Log.d("Get URL : ", url);
                Log.d("Get Result",getResult);
                return JSONUtil.jsonString2Object(getResult);
            }  catch (Exception e) {
                e.printStackTrace();
                return null;
            } finally {
                if (reader != null) {
                    reader.close();
                }

                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }

                if (inputStream != null) {
                    inputStream.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // post方法访问服务器，返回json对象
    public static JSONObject postRequest(Context context, String url, Map<String, String> parameterMap, Boolean enableSession, Boolean loginRequired) {
        try{
            Log.d("POST URL : ", url);
            String parameterString = buildParameterString(parameterMap, loginRequired);
            Log.d("POST parameter", parameterString);

            final URL localURL = new URL(url);

            URLConnection connection = localURL.openConnection();

            HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
            /*
            // http://developer.android.com/training/articles/security-ssl.html
            // Create an HostnameVerifier that hardwires the expected hostname.
            // Note that is different than the URL's hostname:
            // example.com versus example.org
            HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    HostnameVerifier hv =
                            HttpsURLConnection.getDefaultHostnameVerifier();
                    return hv.verify(localURL.getHost(), session);
                }
            };
            httpURLConnection.setHostnameVerifier(hostnameVerifier);
            */
            httpURLConnection.setConnectTimeout(15000);
            if(url.equals(APIAddress.PUSH_SERVICE_URL)) {
                httpURLConnection.setReadTimeout(360000);
            }else{
                httpURLConnection.setReadTimeout(25000);
            }
            // 设置是否向httpUrlConnection输出，因为这个是post请求，参数要放在
            // http正文内，因此需要设为true, 默认情况下是false;
            httpURLConnection.setDoOutput(true);
            // 设置是否从httpUrlConnection读入，默认情况下是true;
            httpURLConnection.setDoInput(true);
            httpURLConnection.setInstanceFollowRedirects(true);//允许重定向
            // Post 请求不能使用缓存
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Accept-Charset", charset);
            /*
            if (Build.VERSION.SDK_INT < 21) {
                // http://stackoverflow.com/questions/17638398/androids-httpurlconnection-throws-eofexception-on-head-requests
                // Fixed known bug in Android's class implementation
                httpURLConnection.setRequestProperty("Accept-Encoding", "");
            }
            */
            httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpURLConnection.setRequestProperty("Content-Length", String.valueOf(parameterString.length()));
            String cookie = getCookie(context);
            if(enableSession && cookie != null){
                httpURLConnection.setRequestProperty("Cookie", cookie);
            }
            /*
            if (Build.VERSION.SDK_INT < 21) {
                //http://stackoverflow.com/questions/15411213/android-httpsurlconnection-eofexception
                // Fixed bug with recycled url connections in versions of android.
                httpURLConnection.setRequestProperty("Connection", "close");
            }
            */
            String tempLine = null;
            OutputStream outputStream = httpURLConnection.getOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);//现在通过输出流对象构建对象输出流对象，以实现输出可序列化的对象。
            outputStreamWriter.write(parameterString);// 向对象输出流写出数据，这些数据将存到内存缓冲区中
            outputStreamWriter.flush();// 刷新对象输出流，将任何字节都写入潜在的流中（些处为ObjectOutputStream）
            outputStreamWriter.close();
            outputStream.close();

            switch (httpURLConnection.getResponseCode()){
                case HttpURLConnection.HTTP_OK:
                case 301:
                case 302:
                case 404:
                    break;
                case 403:
                    Log.d("Configuration error", "API_KEY or API_SECRET or system time error.");
                    return null;
                case 401:
                    Log.d("Post Result","Code 401");
                    CarbonForumApplication.userInfo.edit().clear().apply();
                    Intent intent = new Intent(context, LoginActivity.class);
                    context.startActivity(intent);
                    break;
                case 500:
                    Log.d("Post Result","Code 500");
                    return null;
                default:
                    throw new Exception("HTTP Request is not success, Response code is " + httpURLConnection.getResponseCode());
            }
            if(enableSession) {
                saveCookie(context, httpURLConnection);
            }
            InputStream inputStream = httpURLConnection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            StringBuilder resultBuffer = new StringBuilder();

            while ((tempLine = reader.readLine()) != null) {
                resultBuffer.append(tempLine);
            }
            reader.close();
            inputStreamReader.close();
            inputStream.close();

            //httpURLConnection.disconnect();//断开连接
            String postResult = resultBuffer.toString();
            Log.d("Post Result",postResult);
            JSONTokener jsonParser = new JSONTokener(postResult);
            return (JSONObject) jsonParser.nextValue();

        } catch (Exception e) {
            Log.d("Post Error", "No Network");
            e.printStackTrace();
            return null;
        }
    }

    //获取之前保存的Cookie
    public static String getCookie(Context context){
        SharedPreferences mySharedPreferences= context.getSharedPreferences("Session",
                Activity.MODE_PRIVATE);
        try{
            return  mySharedPreferences.getString("Cookie", "");
        } catch (ClassCastException e) {
            e.printStackTrace();
            return null;
        }
    }

    //保存Cookie
    public static Boolean saveCookie(Context context, URLConnection connection){
        //获取Cookie
        String headerName=null;
        for (int i=1; (headerName = connection.getHeaderFieldKey(i))!=null; i++) {
            if (headerName.equals("Set-Cookie")) {
                String cookie = connection.getHeaderField(i);
                //将Cookie保存起来
                SharedPreferences mySharedPreferences = context.getSharedPreferences("Session",
                        Activity.MODE_PRIVATE);
                SharedPreferences.Editor editor = mySharedPreferences.edit();
                editor.putString("Cookie", cookie);
                editor.apply();
                return true;
            }
        }
        return false;
    }

    public static String buildParameterString(Map<String, String> parameterMap, Boolean loginRequired){
        /* Translate parameter map to parameter date string */
        StringBuilder parameterBuffer = new StringBuilder();
        String currentTimeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        parameterBuffer
                .append("SKey").append("=")
                .append(APIAddress.API_KEY)
                .append("&")
                .append("STime").append("=")
                .append(currentTimeStamp)
                .append("&")
                .append("SValue").append("=")
                .append(MD5Util.md5(APIAddress.API_KEY + APIAddress.API_SECRET + currentTimeStamp));

        if(loginRequired && CarbonForumApplication.isLoggedIn()){
            parameterBuffer
                    .append("&")
                    .append("AuthUserID").append("=")
                    .append(CarbonForumApplication.userInfo.getString("UserID", ""))
                    .append("&")
                    .append("AuthUserExpirationTime").append("=")
                    .append(CarbonForumApplication.userInfo.getString("UserExpirationTime", ""))
                    .append("&")
                    .append("AuthUserCode").append("=")
                    .append(CarbonForumApplication.userInfo.getString("UserCode", ""));
        }
        if (parameterMap != null) {
            parameterBuffer.append("&");
            Iterator iterator = parameterMap.keySet().iterator();
            String key = null;
            String value = null;
            while (iterator.hasNext()) {
                key = (String) iterator.next();
                if (parameterMap.get(key) != null) {
                    try {
                        value = URLEncoder.encode(parameterMap.get(key), "UTF-8");
                    }catch(UnsupportedEncodingException e){
                        value = parameterMap.get(key);
                        e.printStackTrace();
                    }
                } else {
                    value = "";
                }
                parameterBuffer.append(key.contains("#") ? key.substring(0, key.indexOf("#")) : key).append("=").append(value);
                if (iterator.hasNext()) {
                    parameterBuffer.append("&");
                }
            }
        }
        return parameterBuffer.toString();
    }

    private URLConnection openConnection(URL localURL) throws IOException {
        URLConnection connection;
        if (proxyHost != null && proxyPort != null) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            connection = localURL.openConnection(proxy);
        } else {
            connection = localURL.openConnection();
        }
        return connection;
    }

    private void renderRequest(URLConnection connection) {

        if (connectTimeout != null) {
            connection.setConnectTimeout(connectTimeout);
        }

        if (socketTimeout != null) {
            connection.setReadTimeout(socketTimeout);
        }

    }

    /*
     * Getter & Setter
     */
    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Integer getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(Integer socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public Integer getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(Integer proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        HttpUtil.charset = charset;
    }
}

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by 灿斌 on 10/10/2015.
 */
public class JSONUtil {
    // JSON字符串转List
    public static List<Map<String, Object>> jsonObject2List(JSONObject jsonObject, String keyName) {

        List<Map<String, Object>> list = new ArrayList<>();
        if(null != jsonObject){
            try {
                JSONArray jsonArray = jsonObject.getJSONArray(keyName);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject2 = jsonArray.getJSONObject(i);
                    Map<String, Object> map = new HashMap<>();
                    Iterator<String> iterator = jsonObject2.keys();
                    while (iterator.hasNext()) {
                        String key = iterator.next();
                        Object value = jsonObject2.get(key);
                        map.put(key, value);
                    }
                    list.add(map);
                }
                return list;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }else{
            return null;
        }
    }
    public static JSONObject jsonString2Object(String jsonString){
        try {
            JSONTokener jsonParser = new JSONTokener(jsonString);
            return (JSONObject) jsonParser.nextValue();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}


import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by 灿斌 on 10/11/2015.
 */
public class MD5Util {
    public static String md5(String string) {
        byte[] hash;

        try {
            hash = MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Huh, MD5 should be supported?", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Huh, UTF-8 should be supported?", e);
        }

        StringBuilder hex = new StringBuilder(hash.length * 2);

        for (byte b : hash) {
            int i = (b & 0xFF);
            if (i < 0x10) hex.append('0');
            hex.append(Integer.toHexString(i));
        }

        return hex.toString();
    }
}


import android.content.Context;

import com.lincanbin.carbonforum.R;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by 灿斌 on 5/17/2015.
 */
public class TimeUtil {
    public static String formatTime(Context context, long unixTimeStamp){
        long seconds = System.currentTimeMillis() / 1000 - unixTimeStamp;
        if (seconds < 2592000) {
            // 小于30天如下显示
            if (seconds >= 86400) {
                return Long.toString(seconds / 86400) + " " + context.getString(R.string.days_ago);
            } else if (seconds >= 3600) {
                return Long.toString(seconds / 3600) + " " + context.getString(R.string.hours_ago);
            } else if (seconds >= 60) {
                return Long.toString(seconds / 60) + " " + context.getString(R.string.minutes_ago);
            } else if (seconds < 0) {
                return context.getString(R.string.just_now);
            } else {
                return Long.toString(seconds + 1) + " " + context.getString(R.string.seconds_ago);
            }
        } else {
            // 大于30天直接显示日期
            Date nowTime = new Date(unixTimeStamp*1000);
            return DateFormat.getDateInstance().format(nowTime);
        }
    }

}

/*
Copyright (c) 2005, Pete Bevin.
<http://markdownj.petebevin.com>

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

* Redistributions of source code must retain the above copyright notice,
  this list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution.

* Neither the name "Markdown" nor the names of its contributors may
  be used to endorse or promote products derived from this software
  without specific prior written permission.

This software is provided by the copyright holders and contributors "as
is" and any express or implied warranties, including, but not limited
to, the implied warranties of merchantability and fitness for a
particular purpose are disclaimed. In no event shall the copyright owner
or contributors be liable for any direct, indirect, incidental, special,
exemplary, or consequential damages (including, but not limited to,
procurement of substitute goods or services; loss of use, data, or
profits; or business interruption) however caused and on any theory of
liability, whether in contract, strict liability, or tort (including
negligence or otherwise) arising in any way out of the use of this
software, even if advised of the possibility of such damage.

*/

import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CharacterProtector {
    private final ConcurrentMap<String, String> protectMap = new ConcurrentHashMap<String, String>();
    private final ConcurrentMap<String, String> unprotectMap = new ConcurrentHashMap<String, String>();
    private static final String GOOD_CHARS = "0123456789qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM";
    private Random rnd = new Random();


    public String encode(String literal) {
        String encoded = protectMap.get(literal);
        if (encoded == null) {
            synchronized (protectMap) {
                encoded = protectMap.get(literal);
                if (encoded == null) {
                    encoded = addToken(literal);
                }
            }
        }
        return encoded;
    }

    public String decode(String coded) {
        return unprotectMap.get(coded);
    }

    public Collection<String> getAllEncodedTokens() {
        return Collections.unmodifiableSet(unprotectMap.keySet());
    }

    private String addToken(String literal) {
        String encoded = longRandomString();

        protectMap.put(literal, encoded);
        unprotectMap.put(encoded, literal);

        return encoded;
    }

    private String longRandomString() {
        StringBuilder sb = new StringBuilder();
        final int CHAR_MAX = GOOD_CHARS.length();
        for (int i = 0; i < 20; i++) {
            sb.append(GOOD_CHARS.charAt(rnd.nextInt(CHAR_MAX)));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return protectMap.toString();
    }
}

/*
Copyright (c) 2005, Martian Software
Authors: Pete Bevin, John Mutchek
http://www.martiansoftware.com/markdownj

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

* Redistributions of source code must retain the above copyright notice,
  this list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution.

* Neither the name "Markdown" nor the names of its contributors may
  be used to endorse or promote products derived from this software
  without specific prior written permission.

This software is provided by the copyright holders and contributors "as
is" and any express or implied warranties, including, but not limited
to, the implied warranties of merchantability and fitness for a
particular purpose are disclaimed. In no event shall the copyright owner
or contributors be liable for any direct, indirect, incidental, special,
exemplary, or consequential damages (including, but not limited to,
procurement of substitute goods or services; loss of use, data, or
profits; or business interruption) however caused and on any theory of
liability, whether in contract, strict liability, or tort (including
negligence or otherwise) arising in any way out of the use of this
software, even if advised of the possibility of such damage.

*/


import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convert Markdown text into HTML, as per http://daringfireball.net/projects/markdown/ .
 * Usage:
 * <pre><code>
 *     MarkdownProcessor markdown = new MarkdownProcessor();
 *     String html = markdown.markdown("*italic*   **bold**\n_italic_   __bold__");
 * </code></pre>
 */
public class MarkdownProcessor {
    private Random rnd = new Random();
    private Map<String, LinkDefinition> linkDefinitions = new TreeMap<String, LinkDefinition>();
    private static final CharacterProtector HTML_PROTECTOR = new CharacterProtector();
    private static final CharacterProtector CHAR_PROTECTOR = new CharacterProtector();
    private int listLevel;
    private int tabWidth = 4;

    /**
     * Creates a new Markdown processor.
     */
    public MarkdownProcessor() {
        listLevel = 0;
    }

    /**
     * Perform the conversion from Markdown to HTML.
     *
     * @param txt - input in markdown format
     * @return HTML block corresponding to txt passed in.
     */
    public String markdown(String txt) {
        if (txt == null) {
            txt = "";
        }
        TextEditor text = new TextEditor(txt);

        // Standardize line endings:
        text.replaceAll("\\r\\n", "\n"); 	// DOS to Unix
        text.replaceAll("\\r", "\n");    	// Mac to Unix
        text.replaceAll("^[ \\t]+$", "");

        // Make sure $text ends with a couple of newlines:
        text.append("\n\n");

        text.detabify();
        text.deleteAll("^[ ]+$");
        hashHTMLBlocks(text);
        stripLinkDefinitions(text);
        text = runBlockGamut(text);
        unEscapeSpecialChars(text);

        text.append("\n");
        return text.toString();
    }

    private TextEditor encodeBackslashEscapes(TextEditor text) {
        char[] normalChars = "`_>!".toCharArray();
        char[] escapedChars = "*{}[]()#+-.".toCharArray();

        // Two backslashes in a row
        text.replaceAllLiteral("\\\\\\\\", CHAR_PROTECTOR.encode("\\"));

        // Normal characters don't require a backslash in the regular expression
        encodeEscapes(text, normalChars, "\\\\");
        encodeEscapes(text, escapedChars, "\\\\\\");

        return text;
    }

    private TextEditor encodeEscapes(TextEditor text, char[] chars, String slashes) {
        for (char ch : chars) {
            String regex = slashes + ch;
            text.replaceAllLiteral(regex, CHAR_PROTECTOR.encode(String.valueOf(ch)));
        }
        return text;
    }

    private void stripLinkDefinitions(com.lincanbin.carbonforum.util.markdown.TextEditor text) {
        Pattern p = Pattern.compile("^[ ]{0,3}\\[(.+)\\]:" + // ID = $1
                "[ \\t]*\\n?[ \\t]*" + // Space
                "<?(\\S+?)>?" + // URL = $2
                "[ \\t]*\\n?[ \\t]*" + // Space
                "(?:[\"(](.+?)[\")][ \\t]*)?" + // Optional title = $3
                "(?:\\n+|\\Z)",
                Pattern.MULTILINE);

        text.replaceAll(p, new com.lincanbin.carbonforum.util.markdown.Replacement() {
            public String replacement(Matcher m) {
                String id = m.group(1).toLowerCase();
                String url = encodeAmpsAndAngles(new com.lincanbin.carbonforum.util.markdown.TextEditor(m.group(2))).toString();
                String title = m.group(3);

                if (title == null) {
                    title = "";
                }
                title = replaceAll(title, "\"", "&quot;");
                linkDefinitions.put(id, new LinkDefinition(url, title));
                return "";
            }
        });
    }

    public com.lincanbin.carbonforum.util.markdown.TextEditor runBlockGamut(com.lincanbin.carbonforum.util.markdown.TextEditor text) {
        doHeaders(text);
        doHorizontalRules(text);
        doLists(text);
        doCodeBlocks(text);
        doBlockQuotes(text);

        hashHTMLBlocks(text);

        return formParagraphs(text);
    }

    private void doHorizontalRules(com.lincanbin.carbonforum.util.markdown.TextEditor text) {
        String[] hrDelimiters = {"\\*", "-", "_"};
        for (String hrDelimiter : hrDelimiters) {
            text.replaceAll("^[ ]{0,2}([ ]?" + hrDelimiter + "[ ]?){3,}[ ]*$", "<hr />");
        }
    }

    private void hashHTMLBlocks(com.lincanbin.carbonforum.util.markdown.TextEditor text) {
        // Hashify HTML blocks:
        // We only want to do this for block-level HTML tags, such as headers,
        // lists, and tables. That's because we still want to wrap <p>s around
        // "paragraphs" that are wrapped in non-block-level tags, such as anchors,
        // phrase emphasis, and spans. The list of tags we're looking for is
        // hard-coded:

        String[] tagsA = {
            "p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "pre", "table",
            "dl", "ol", "ul", "script", "noscript", "form", "fieldset", "iframe", "math"
        };
        String[] tagsB = {"ins", "del"};

        String alternationA = join("|", tagsA);
        String alternationB = alternationA + "|" + join("|", tagsB);

        int less_than_tab = tabWidth - 1;

        // First, look for nested blocks, e.g.:
        //   <div>
        //       <div>
        //       tags for inner block must be indented.
        //       </div>
        //   </div>
        //
        // The outermost tags must start at the left margin for this to match, and
        // the inner nested divs must be indented.
        // We need to do this before the next, more liberal match, because the next
        // match will start at the first `<div>` and stop at the first `</div>`.
        Pattern p1 = Pattern.compile("(" +
                "^<(" + alternationA + ")" +
                "\\b" +
                "(.*\\n)*?" +
                "</\\2>" +
                "[ ]*" +
                "(?=\\n+|\\Z))", Pattern.MULTILINE |  Pattern.CASE_INSENSITIVE);

        com.lincanbin.carbonforum.util.markdown.Replacement protectHTML = new com.lincanbin.carbonforum.util.markdown.Replacement() {
            public String replacement(Matcher m) {
                String literal = m.group();
                return "\n\n" + HTML_PROTECTOR.encode(literal) + "\n\n";
            }
        };
        text.replaceAll(p1, protectHTML);

        // Now match more liberally, simply from `\n<tag>` to `</tag>\n`
        Pattern p2 = Pattern.compile("(" +
                "^" +
                "<(" + alternationB + ")" +
                "\\b" +
                "(.*\\n)*?" +
                ".*</\\2>" +
                "[ ]*" +
                "(?=\\n+|\\Z))", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        text.replaceAll(p2, protectHTML);

        // Special case for <hr>
        Pattern p3 = Pattern.compile("(?:" +
                "(?<=\\n\\n)" +
                "|" +
                "\\A\\n?" +
                ")" +
                "(" +
                "[ ]{0," + less_than_tab + "}" +
                "<(hr)" +
                "\\b" +
                "([^<>])*?" +
                "/?>" +
                "[ ]*" +
                "(?=\\n{2,}|\\Z))", Pattern.CASE_INSENSITIVE);
        text.replaceAll(p3, protectHTML);

        // Special case for standalone HTML comments:
        Pattern p4 = Pattern.compile("(?:" +
                "(?<=\\n\\n)" +
                "|" +
                "\\A\\n?" +
                ")" +
                "(" +
                "[ ]{0," + less_than_tab + "}" +
                "(?s:" +
                "<!" +
                "(--.*?--\\s*)+" +
                ">" +
                ")" +
                "[ ]*" +
                "(?=\\n{2,}|\\Z)" +
                ")");
        text.replaceAll(p4, protectHTML);
    }

    private com.lincanbin.carbonforum.util.markdown.TextEditor formParagraphs(com.lincanbin.carbonforum.util.markdown.TextEditor markup) {
        markup.deleteAll("\\A\\n+");
        markup.deleteAll("\\n+\\z");

        String[] paragraphs;
        if (markup.isEmpty()) {
            paragraphs = new String[0];
        } else {
            paragraphs = Pattern.compile("\\n{2,}").split(markup.toString());
        }
        for (int i = 0; i < paragraphs.length; i++) {
            String paragraph = paragraphs[i];
            String decoded = HTML_PROTECTOR.decode(paragraph);
            if (decoded != null) {
                paragraphs[i] = decoded;
            } else {
                paragraph = runSpanGamut(new com.lincanbin.carbonforum.util.markdown.TextEditor(paragraph)).toString();
                paragraphs[i] = "<p>" + paragraph + "</p>";
            }
        }
        return new com.lincanbin.carbonforum.util.markdown.TextEditor(join("\n\n", paragraphs));
    }


    private com.lincanbin.carbonforum.util.markdown.TextEditor doAutoLinks(com.lincanbin.carbonforum.util.markdown.TextEditor markup) {
        markup.replaceAll("<((https?|ftp):[^'\">\\s]+)>", "<a href=\"$1\">$1</a>");
        Pattern email = Pattern.compile("<([-.\\w]+\\@[-a-z0-9]+(\\.[-a-z0-9]+)*\\.[a-z]+)>");
        markup.replaceAll(email, new com.lincanbin.carbonforum.util.markdown.Replacement() {
            public String replacement(Matcher m) {
                String address = m.group(1);
                com.lincanbin.carbonforum.util.markdown.TextEditor ed = new com.lincanbin.carbonforum.util.markdown.TextEditor(address);
                unEscapeSpecialChars(ed);
                String addr = encodeEmail(ed.toString());
                String url = encodeEmail("mailto:" + ed.toString());
                return "<a href=\"" + url + "\">" + addr + "</a>";
            }
        });
        return markup;
    }

    private void unEscapeSpecialChars(com.lincanbin.carbonforum.util.markdown.TextEditor ed) {
        for (String hash : CHAR_PROTECTOR.getAllEncodedTokens()) {
            String plaintext = CHAR_PROTECTOR.decode(hash);
            ed.replaceAllLiteral(hash, plaintext);
        }
    }

    private String encodeEmail(String s) {
        StringBuilder sb = new StringBuilder();
        char[] email = s.toCharArray();
        for (char ch : email) {
            double r = rnd.nextDouble();
            if (r < 0.45) {      // Decimal
                sb.append("&#");
                sb.append((int) ch);
                sb.append(';');
            } else if (r < 0.9) {  // Hex
                sb.append("&#x");
                sb.append(Integer.toString((int) ch, 16));
                sb.append(';');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private com.lincanbin.carbonforum.util.markdown.TextEditor doBlockQuotes(com.lincanbin.carbonforum.util.markdown.TextEditor markup) {
        Pattern p = Pattern.compile("(" +
                "(" +
                "^[ \t]*>[ \t]?" + // > at the start of a line
                ".+\\n" + // rest of the first line
                "(.+\\n)*" + // subsequent consecutive lines
                "\\n*" + // blanks
                ")+" +
                ")", Pattern.MULTILINE);
        return markup.replaceAll(p, new com.lincanbin.carbonforum.util.markdown.Replacement() {
            public String replacement(Matcher m) {
                com.lincanbin.carbonforum.util.markdown.TextEditor blockQuote = new com.lincanbin.carbonforum.util.markdown.TextEditor(m.group(1));
                blockQuote.deleteAll("^[ \t]*>[ \t]?");
                blockQuote.deleteAll("^[ \t]+$");
                blockQuote = runBlockGamut(blockQuote);
                blockQuote.replaceAll("^", "  ");


                Pattern p1 = Pattern.compile("(\\s*<pre>.*?</pre>)", Pattern.DOTALL);
                blockQuote = blockQuote.replaceAll(p1, new com.lincanbin.carbonforum.util.markdown.Replacement() {
                    public String replacement(Matcher m1) {
                        String pre = m1.group(1);
                        return deleteAll(pre, "^  ");
                    }
                });
                return "<blockquote>\n" + blockQuote + "\n</blockquote>\n\n";
            }
        });
    }

    private com.lincanbin.carbonforum.util.markdown.TextEditor doCodeBlocks(com.lincanbin.carbonforum.util.markdown.TextEditor markup) {
        Pattern p = Pattern.compile("" +
                "(?:\\n\\n|\\A)" +
                "((?:" +
                "(?:[ ]{4})" +
                ".*\\n+" +
                ")+" +
                ")" +
                "((?=^[ ]{0,4}\\S)|\\Z)", Pattern.MULTILINE);
        return markup.replaceAll(p, new com.lincanbin.carbonforum.util.markdown.Replacement() {
        			private static final String LANG_IDENTIFIER = "lang:";
                    public String replacement(Matcher m) {
                        String codeBlock = m.group(1);
                        com.lincanbin.carbonforum.util.markdown.TextEditor ed = new com.lincanbin.carbonforum.util.markdown.TextEditor(codeBlock);
                        ed.outdent();
                        encodeCode(ed);
                        ed.detabify().deleteAll("\\A\\n+").deleteAll("\\s+\\z");
                        String text = ed.toString();
                        String out;
                        String firstLine = firstLine(text);
                        if (isLanguageIdentifier(firstLine)) {
                          out = languageBlock(firstLine, text);
                        } else {
                          out = genericCodeBlock(text);
                        }
                        return out;
                    }

                    public String firstLine(String text)
                    {
                        if (text == null) {
                            return "";
                        }
                        String[] splitted = text.split("\\n");
                        return splitted[0];
                    }

                    public boolean isLanguageIdentifier(String line)
                    {
                        if (line == null) {
                            return false;
                        }
                        String lang = "";
                        if (line.startsWith(LANG_IDENTIFIER)) {
                        	lang = line.replaceFirst(LANG_IDENTIFIER, "").trim();
                        }
                        return lang.length() > 0;
                    }

                    public String languageBlock(String firstLine, String text)
                    {
                        // dont'use %n in format string (markdown aspect every new line char as "\n")
                    	//String codeBlockTemplate = "<pre class=\"brush: %s\">%n%s%n</pre>"; // http://alexgorbatchev.com/wiki/SyntaxHighlighter
                        String codeBlockTemplate = "\n\n<pre class=\"%s\">\n%s\n</pre>\n\n"; // http://shjs.sourceforge.net/doc/documentation.html
                        String lang = firstLine.replaceFirst(LANG_IDENTIFIER, "").trim();
                        String block = text.replaceFirst( firstLine+"\n", "");
                        return String.format(codeBlockTemplate, lang, block);
                    }
                    public String genericCodeBlock(String text)
                    {
                        // dont'use %n in format string (markdown aspect every new line char as "\n")
                    	String codeBlockTemplate = "\n\n<pre><code>%s\n</code></pre>\n\n";
                        return String.format(codeBlockTemplate, text);
                    }
                });
    }

    private void encodeCode(com.lincanbin.carbonforum.util.markdown.TextEditor ed) {
        ed.replaceAll("&", "&amp;");
        ed.replaceAll("<", "&lt;");
        ed.replaceAll(">", "&gt;");
        ed.replaceAll("\\*", CHAR_PROTECTOR.encode("*"));
        ed.replaceAll("_", CHAR_PROTECTOR.encode("_"));
        ed.replaceAll("\\{", CHAR_PROTECTOR.encode("{"));
        ed.replaceAll("\\}", CHAR_PROTECTOR.encode("}"));
        ed.replaceAll("\\[", CHAR_PROTECTOR.encode("["));
        ed.replaceAll("\\]", CHAR_PROTECTOR.encode("]"));
        ed.replaceAll("\\\\", CHAR_PROTECTOR.encode("\\"));
    }

    private TextEditor doLists(TextEditor text) {
        int lessThanTab = tabWidth - 1;

        String wholeList =
                "(" +
                "(" +
                "[ ]{0," + lessThanTab + "}" +
                "((?:[-+*]|\\d+[.]))" + // $3 is first list item marker
                "[ ]+" +
                ")" +
                "(?s:.+?)" +
                "(" +
                "\\z" + // End of input is OK
                "|" +
                "\\n{2,}" +
                "(?=\\S)" + // If not end of input, then a new para
                "(?![ ]*" +
                "(?:[-+*]|\\d+[.])" +
                "[ ]+" +
                ")" + // negative lookahead for another list marker
                ")" +
                ")";

        if (listLevel > 0) {
            Replacement replacer = new Replacement() {
                public String replacement(Matcher m) {
                    String list = m.group(1);
                    String listStart = m.group(3);
                    String listType;

                    if (listStart.matches("[*+-]")) {
                        listType = "ul";
                    } else {
                        listType = "ol";
                    }

                    // Turn double returns into triple returns, so that we can make a
                    // paragraph for the last item in a list, if necessary:
                    list = replaceAll(list, "\\n{2,}", "\n\n\n");

                    String result = processListItems(list);

                    // Trim any trailing whitespace, to put the closing `</ol>` or `</ul>`
                    // up on the preceding line, to get it past the current stupid
                    // HTML block parser. This is a hack to work around the terrible
                    // hack that is the HTML block parser.
                    result = result.replaceAll("\\s+$", "");

                    String html;
                    if ("ul".equals(listType)) {
                        html = "<ul>" + result + "</ul>\n";
                    } else {
                        html = "<ol>" + result + "</ol>\n";
                    }
                    return html;
                }
            };
            Pattern matchStartOfLine = Pattern.compile("^" + wholeList, Pattern.MULTILINE);
            text.replaceAll(matchStartOfLine, replacer);
        } else {
            Replacement replacer = new Replacement() {
                public String replacement(Matcher m) {
                    String list = m.group(1);
                    String listStart = m.group(3);
                    String listType = "";

                    if (listStart.matches("[*+-]")) {
                     listType = "ul";
                    } else {
                     listType = "ol";
                    }

                    // Turn double returns into triple returns, so that we can make a
                    // paragraph for the last item in a list, if necessary:
                    list = replaceAll(list, "\n{2,}", "\n\n\n");

                    String result = processListItems(list);

                    String html;
                    if (listStart.matches("[*+-]")) {
                        html = "<ul>\n" + result + "</ul>\n";
                    } else {
                        html = "<ol>\n" + result + "</ol>\n";
                    }
                    return html;
                }
            };
            Pattern matchStartOfLine = Pattern.compile("(?:(?<=\\n\\n)|\\A\\n?)" + wholeList, Pattern.MULTILINE);
            text.replaceAll(matchStartOfLine, replacer);

        }

        return text;
    }

    private String processListItems(String list) {
        // The listLevel variable keeps track of when we're inside a list.
        // Each time we enter a list, we increment it; when we leave a list,
        // we decrement. If it's zero, we're not in a list anymore.
        //
        // We do this because when we're not inside a list, we want to treat
        // something like this:
        //
        //       I recommend upgrading to version
        //       8. Oops, now this line is treated
        //       as a sub-list.
        //
        // As a single paragraph, despite the fact that the second line starts
        // with a digit-period-space sequence.
        //
        // Whereas when we're inside a list (or sub-list), that line will be
        // treated as the start of a sub-list. What a kludge, huh? This is
        // an aspect of Markdown's syntax that's hard to parse perfectly
        // without resorting to mind-reading. Perhaps the solution is to
        // change the syntax rules such that sub-lists must start with a
        // starting cardinal number; e.g. "1." or "a.".
        listLevel++;

        // Trim trailing blank lines:
        list = replaceAll(list, "\\n{2,}\\z", "\n");

        Pattern p = Pattern.compile("(\\n)?" +
                "^([ \\t]*)([-+*]|\\d+[.])[ ]+" +
                "((?s:.+?)(\\n{1,2}))" +
                "(?=\\n*(\\z|\\2([-+\\*]|\\d+[.])[ \\t]+))",
                Pattern.MULTILINE);
        list = replaceAll(list, p, new Replacement() {
            public String replacement(Matcher m) {
                String text = m.group(4);
                TextEditor item = new TextEditor(text);
                String leadingLine = m.group(1);
                if (!isEmptyString(leadingLine) || hasParagraphBreak(item)) {
                    item = runBlockGamut(item.outdent());
                } else {
                    // Recurse sub-lists
                    item = doLists(item.outdent());
                    item = runSpanGamut(item);
                }
                return "<li>" + item.trim().toString() + "</li>\n";
            }
        });
        listLevel--;
        return list;
    }

    private boolean hasParagraphBreak(TextEditor item) {
        return item.toString().indexOf("\n\n") != -1;
    }

    private boolean isEmptyString(String leadingLine) {
        return leadingLine == null || leadingLine.equals("");
    }

    private TextEditor doHeaders(TextEditor markup) {
        // setext-style headers
        markup.replaceAll("^(.*)\n====+$", "<h1>$1</h1>");
        markup.replaceAll("^(.*)\n----+$", "<h2>$1</h2>");

        // atx-style headers - e.g., "#### heading 4 ####"
        Pattern p = Pattern.compile("^(#{1,6})\\s*(.*?)\\s*\\1?$", Pattern.MULTILINE);
        markup.replaceAll(p, new Replacement() {
            public String replacement(Matcher m) {
                String marker = m.group(1);
                String heading = m.group(2);
                int level = marker.length();
                String tag = "h" + level;
                return "<" + tag + ">" + heading + "</" + tag + ">\n";
            }
        });
        return markup;
    }

    private String join(String separator, String[] strings) {
        int length = strings.length;
        StringBuilder buf = new StringBuilder();
        if (length > 0) {
            buf.append(strings[0]);
            for (int i = 1; i < length; i++) {
                buf.append(separator).append(strings[i]);
            }
        }
        return buf.toString();
    }

    public TextEditor runSpanGamut(TextEditor text) {
        text = escapeSpecialCharsWithinTagAttributes(text);
        text = doCodeSpans(text);
        text = encodeBackslashEscapes(text);

        doImages(text);
        doAnchors(text);
        doAutoLinks(text);

        // Fix for BUG #1357582
        // We must call escapeSpecialCharsWithinTagAttributes() a second time to
        // escape the contents of any attributes generated by the prior methods.
        // - Nathan Winant, nw@exegetic.net, 8/29/2006
        text = escapeSpecialCharsWithinTagAttributes(text);

        encodeAmpsAndAngles(text);
        doItalicsAndBold(text);

        // Manual line breaks
        text.replaceAll(" {2,}\n", " <br />\n");
        return text;
    }

    /**
     * escape special characters
     *
     * Within tags -- meaning between < and > -- encode [\ ` * _] so they
     * don't conflict with their use in Markdown for code, italics and strong.
     * We're replacing each such character with its corresponding random string
     * value; this is likely overkill, but it should prevent us from colliding
     * with the escape values by accident.
     *
     * @param text
     * @return
     */
    private TextEditor escapeSpecialCharsWithinTagAttributes(TextEditor text) {
        Collection<HTMLToken> tokens = text.tokenizeHTML();
        TextEditor newText = new TextEditor("");

        for (HTMLToken token : tokens) {
            String value = token.getText();
            if (token.isTag()) {
                value = value.replaceAll("\\\\", CHAR_PROTECTOR.encode("\\"));
                value = value.replaceAll("`", CHAR_PROTECTOR.encode("`"));
                value = value.replaceAll("\\*", CHAR_PROTECTOR.encode("*"));
                value = value.replaceAll("_", CHAR_PROTECTOR.encode("_"));
            }
            newText.append(value);
        }

        return newText;
    }

    private void doImages(TextEditor text) {
        // Inline image syntax
    	text.replaceAll("!\\[(.*)\\]\\((.*) \"(.*)\"\\)", "<img src=\"$2\" alt=\"$1\" title=\"$3\" />");
    	text.replaceAll("!\\[(.*)\\]\\((.*)\\)", "<img src=\"$2\" alt=\"$1\" />");

        // Reference-style image syntax
    	Pattern imageLink = Pattern.compile("(" +
            	"[!]\\[(.*?)\\]" + // alt text = $2
            	"[ ]?(?:\\n[ ]*)?" +
            	"\\[(.*?)\\]" + // ID = $3
            	")");
    	text.replaceAll(imageLink, new Replacement() {
        	public String replacement(Matcher m) {
            	String replacementText;
            	String wholeMatch = m.group(1);
            	String altText = m.group(2);
            	String id = m.group(3).toLowerCase();
            	if (id == null || "".equals(id)) {
                	id = altText.toLowerCase();
            	}

            	// imageDefinition is the same as linkDefinition
            	LinkDefinition defn = linkDefinitions.get(id);
            	if (defn != null) {
                	String url = defn.getUrl();
                	url = url.replaceAll("\\*", CHAR_PROTECTOR.encode("*"));
                	url = url.replaceAll("_", CHAR_PROTECTOR.encode("_"));
                	String title = defn.getTitle();
                	String titleTag = "";
                	if (title != null && !title.equals("")) {
                    	title = title.replaceAll("\\*", CHAR_PROTECTOR.encode("*"));
                    	title = title.replaceAll("_", CHAR_PROTECTOR.encode("_"));
                    	titleTag = " alt=\"" + altText + "\" title=\"" + title + "\"";
                	}
                	replacementText = "<img src=\"" + url + "\"" + titleTag + "/>";
            	} else {
                	replacementText = wholeMatch;
            	}
            	return replacementText;
        	}
    	});
	}

    private TextEditor doAnchors(TextEditor markup) {
        // Internal references: [link text] [id]
        Pattern internalLink = Pattern.compile("(" +
                "\\[(.*?)\\]" + // Link text = $2
                "[ ]?(?:\\n[ ]*)?" +
                "\\[(.*?)\\]" + // ID = $3
                ")");
        markup.replaceAll(internalLink, new Replacement() {
            public String replacement(Matcher m) {
                String replacementText;
                String wholeMatch = m.group(1);
                String linkText = m.group(2);
                String id = m.group(3).toLowerCase();
                if (id == null || "".equals(id)) { // for shortcut links like [this][]
                    id = linkText.toLowerCase();
                }

                LinkDefinition defn = linkDefinitions.get(id);
                if (defn != null) {
                    String url = defn.getUrl();
                    // protect emphasis (* and _) within urls
                    url = url.replaceAll("\\*", CHAR_PROTECTOR.encode("*"));
                    url = url.replaceAll("_", CHAR_PROTECTOR.encode("_"));
                    String title = defn.getTitle();
                    String titleTag = "";
                    if (title != null && !title.equals("")) {
                        // protect emphasis (* and _) within urls
                        title = title.replaceAll("\\*", CHAR_PROTECTOR.encode("*"));
                        title = title.replaceAll("_", CHAR_PROTECTOR.encode("_"));
                        titleTag = " title=\"" + title + "\"";
                    }
                    replacementText = "<a href=\"" + url + "\"" + titleTag + ">" + linkText + "</a>";
                } else {
                    replacementText = wholeMatch;
                }
                return replacementText;
            }
        });

        // Inline-style links: [link text](url "optional title")
        Pattern inlineLink = Pattern.compile("(" + // Whole match = $1
                "\\[(.*?)\\]" + // Link text = $2
                "\\(" +
                "[ \\t]*" +
                "<?(.*?)>?" + // href = $3
                "[ \\t]*" +
                "(" +
                "(['\"])" + // Quote character = $5
                "(.*?)" + // Title = $6
                "\\5" +
                ")?" +
                "\\)" +
                ")", Pattern.DOTALL);
        markup.replaceAll(inlineLink, new Replacement() {
            public String replacement(Matcher m) {
                String linkText = m.group(2);
                String url = m.group(3);
                String title = m.group(6);
                // protect emphasis (* and _) within urls
                url = url.replaceAll("\\*", CHAR_PROTECTOR.encode("*"));
                url = url.replaceAll("_", CHAR_PROTECTOR.encode("_"));
                StringBuilder result = new StringBuilder();
                result.append("<a href=\"").append(url).append("\"");
                if (title != null) {
                    // protect emphasis (* and _) within urls
                    title = title.replaceAll("\\*", CHAR_PROTECTOR.encode("*"));
                    title = title.replaceAll("_", CHAR_PROTECTOR.encode("_"));
                    title = replaceAll(title, "\"", "&quot;");
                    result.append(" title=\"");
                    result.append(title);
                    result.append("\"");
                }
                result.append(">").append(linkText);
                result.append("</a>");
                return result.toString();
            }
        });

        // Last, handle reference-style shortcuts: [link text]
        // These must come last in case you've also got [link test][1]
        // or [link test](/foo)
        Pattern referenceShortcut = Pattern.compile("(" + // wrap whole match in $1
                                                        "\\[" +
                                                        "([^\\[\\]]+)" + // link text = $2; can't contain '[' or ']'
                                                        "\\]" +
                                                    ")", Pattern.DOTALL);
        markup.replaceAll(referenceShortcut, new Replacement() {
            public String replacement(Matcher m) {
                String replacementText;
                String wholeMatch = m.group(1);
                String linkText = m.group(2);
                String id = m.group(2).toLowerCase(); // link id should be lowercase
                id = id.replaceAll("[ ]?\\n", " "); // change embedded newlines into spaces

                LinkDefinition defn = linkDefinitions.get(id.toLowerCase());
                if (defn != null) {
                    String url = defn.getUrl();
                    // protect emphasis (* and _) within urls
                    url = url.replaceAll("\\*", CHAR_PROTECTOR.encode("*"));
                    url = url.replaceAll("_", CHAR_PROTECTOR.encode("_"));
                    String title = defn.getTitle();
                    String titleTag = "";
                    if (title != null && !title.equals("")) {
                        // protect emphasis (* and _) within urls
                        title = title.replaceAll("\\*", CHAR_PROTECTOR.encode("*"));
                        title = title.replaceAll("_", CHAR_PROTECTOR.encode("_"));
                        titleTag = " title=\"" + title + "\"";
                    }
                    replacementText = "<a href=\"" + url + "\"" + titleTag + ">" + linkText + "</a>";
                } else {
                    replacementText = wholeMatch;
                }
                return replacementText;
         }
        });

        return markup;
    }

    private TextEditor doItalicsAndBold(TextEditor markup) {
        markup.replaceAll("(\\*\\*|__)(?=\\S)(.+?[*_]*)(?<=\\S)\\1", "<strong>$2</strong>");
        markup.replaceAll("(\\*|_)(?=\\S)(.+?)(?<=\\S)\\1", "<em>$2</em>");
        return markup;
    }

    private TextEditor encodeAmpsAndAngles(TextEditor markup) {
        // Ampersand-encoding based entirely on Nat Irons's Amputator MT plugin:
        // http://bumppo.net/projects/amputator/
        markup.replaceAll("&(?!#?[xX]?(?:[0-9a-fA-F]+|\\w+);)", "&amp;");
        markup.replaceAll("<(?![a-zA-Z/?\\$!])", "&lt;");
        return markup;
    }

    private TextEditor doCodeSpans(TextEditor markup) {
            return markup.replaceAll(Pattern.compile("(?<!\\\\)(`+)(.+?)(?<!`)\\1(?!`)"), new Replacement() {
                    public String replacement(Matcher m) {
                        String code = m.group(2);
                        TextEditor subEditor = new TextEditor(code);
                        subEditor.deleteAll("^[ \\t]+").deleteAll("[ \\t]+$");
                        encodeCode(subEditor);
                        return "<code>" + subEditor.toString() + "</code>";
                    }
                });
    }


    private String deleteAll(String text, String regex) {
        return replaceAll(text, regex, "");
    }

    private String replaceAll(String text, String regex, String replacement) {
        TextEditor ed = new TextEditor(text);
        ed.replaceAll(regex, replacement);
        return ed.toString();
    }

    private String replaceAll(String markup, Pattern pattern, Replacement replacement) {
        TextEditor ed = new TextEditor(markup);
        ed.replaceAll(pattern, replacement);
        return ed.toString();
    }

    @Override
    public String toString() {
        return "Markdown Processor for Java 0.4.0 (compatible with Markdown 1.0.2b2)";
    }

    public static void main(String[] args) {
        StringBuilder buf = new StringBuilder();
        char[] cbuf = new char[1024];
        java.io.Reader in = new java.io.InputStreamReader(System.in);
        try {
            int charsRead = in.read(cbuf);
            while (charsRead >= 0) {
                buf.append(cbuf, 0, charsRead);
                charsRead = in.read(cbuf);
            }
            System.out.println(new MarkdownProcessor().markdown(buf.toString()));
        } catch (java.io.IOException e) {
            System.err.println("Error reading input: " + e.getMessage());
            System.exit(1);
        }
    }
}

/*
Copyright (c) 2005, Pete Bevin.
<http://markdownj.petebevin.com>

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

* Redistributions of source code must retain the above copyright notice,
  this list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution.

* Neither the name "Markdown" nor the names of its contributors may
  be used to endorse or promote products derived from this software
  without specific prior written permission.

This software is provided by the copyright holders and contributors "as
is" and any express or implied warranties, including, but not limited
to, the implied warranties of merchantability and fitness for a
particular purpose are disclaimed. In no event shall the copyright owner
or contributors be liable for any direct, indirect, incidental, special,
exemplary, or consequential damages (including, but not limited to,
procurement of substitute goods or services; loss of use, data, or
profits; or business interruption) however caused and on any theory of
liability, whether in contract, strict liability, or tort (including
negligence or otherwise) arising in any way out of the use of this
software, even if advised of the possibility of such damage.

*/


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTMLDecoder {
    public static String decode(String html) {
        com.lincanbin.carbonforum.util.markdown.TextEditor ed = new com.lincanbin.carbonforum.util.markdown.TextEditor(html);
        Pattern p1 = Pattern.compile("&#(\\d+);");
        ed.replaceAll(p1, new com.lincanbin.carbonforum.util.markdown.Replacement() {
            public String replacement(Matcher m) {
                String charDecimal = m.group(1);
                char ch = (char) Integer.parseInt(charDecimal);
                return Character.toString(ch);
            }
        });

        Pattern p2 = Pattern.compile("&#x([0-9a-fA-F]+);");
        ed.replaceAll(p2, new com.lincanbin.carbonforum.util.markdown.Replacement() {
            public String replacement(Matcher m) {
                String charHex = m.group(1);
                char ch = (char) Integer.parseInt(charHex, 16);
                return Character.toString(ch);
            }
        });

        return ed.toString();
    }
}

/*
Copyright (c) 2005, Pete Bevin.
<http://markdownj.petebevin.com>

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

* Redistributions of source code must retain the above copyright notice,
  this list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution.

* Neither the name "Markdown" nor the names of its contributors may
  be used to endorse or promote products derived from this software
  without specific prior written permission.

This software is provided by the copyright holders and contributors "as
is" and any express or implied warranties, including, but not limited
to, the implied warranties of merchantability and fitness for a
particular purpose are disclaimed. In no event shall the copyright owner
or contributors be liable for any direct, indirect, incidental, special,
exemplary, or consequential damages (including, but not limited to,
procurement of substitute goods or services; loss of use, data, or
profits; or business interruption) however caused and on any theory of
liability, whether in contract, strict liability, or tort (including
negligence or otherwise) arising in any way out of the use of this
software, even if advised of the possibility of such damage.

*/


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Mutable String with common operations used in Markdown processing.
 */
public class TextEditor {
    private StringBuilder text;

    /**
     * Create a new TextEditor based on the contents of a String or
     * StringBuffer.
     *
     * @param text
     */
    public TextEditor(CharSequence text) {
        this.text = new StringBuilder(text);
    }

    /**
     * Give up the contents of the TextEditor.
     * @return
     */
    @Override
    public String toString() {
        return text.toString();
    }

    /**
     * Replace all occurrences of the regular expression with the replacement.  The replacement string
     * can contain $1, $2 etc. referring to matched groups in the regular expression.
     *
     * @param regex
     * @param replacement
     * @return
     */
    public TextEditor replaceAll(String regex, String replacement) {
        if (text.length() > 0) {
            final String r = replacement;
            Pattern p = Pattern.compile(regex, Pattern.MULTILINE);
            Matcher m = p.matcher(text);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                m.appendReplacement(sb, r);
            }
            m.appendTail(sb);
            text = new StringBuilder(sb.toString());
        }
        return this;
    }

    /**
     * Same as replaceAll(String, String), but does not interpret
     * $1, $2 etc. in the replacement string.
     * @param regex
     * @param replacement
     * @return
     */
    public TextEditor replaceAllLiteral(String regex, final String replacement) {
        return replaceAll(Pattern.compile(regex, Pattern.MULTILINE), new Replacement() {
            public String replacement(Matcher m) {
                return replacement;
            }
        });
    }

    /**
     * Replace all occurrences of the Pattern.  The Replacement object's replace() method is
     * called on each match, and it provides a replacement, which is placed literally
     * (i.e., without interpreting $1, $2 etc.)
     *
     * @param pattern
     * @param replacement
     * @return
     */
    public TextEditor replaceAll(Pattern pattern, Replacement replacement) {
        Matcher m = pattern.matcher(text);
        int lastIndex = 0;
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            sb.append(text.subSequence(lastIndex, m.start()));
            sb.append(replacement.replacement(m));
            lastIndex = m.end();
        }
        sb.append(text.subSequence(lastIndex, text.length()));
        text = sb;
        return this;
    }

    /**
     * Remove all occurrences of the given regex pattern, replacing them
     * with the empty string.
     *
     * @param pattern Regular expression
     * @return
     * @see java.util.regex.Pattern
     */
    public TextEditor deleteAll(String pattern) {
        return replaceAll(pattern, "");
    }

    /**
     * Convert tabs to spaces given the default tab width of 4 spaces.
     * @return
     */
    public TextEditor detabify() {
        return detabify(4);
    }

    /**
     * Convert tabs to spaces.
     *
     * @param tabWidth  Number of spaces per tab.
     * @return
     */
    public TextEditor detabify(final int tabWidth) {
        replaceAll(Pattern.compile("(.*?)\\t"), new Replacement() {
            public String replacement(Matcher m) {
                String lineSoFar = m.group(1);
                int width = lineSoFar.length();
                StringBuilder replacement = new StringBuilder(lineSoFar);
                do {
                    replacement.append(' ');
                    ++width;
                } while (width % tabWidth != 0);
                return replacement.toString();
            }
        });
        return this;
    }

    /**
     * Remove a number of spaces at the start of each line.
     * @param spaces
     * @return
     */
    public TextEditor outdent(int spaces) {
        return deleteAll("^(\\t|[ ]{1," + spaces + "})");
    }

    /**
     * Remove one tab width (4 spaces) from the start of each line.
     * @return
     */
    public TextEditor outdent() {
        return outdent(4);
    }

    /**
     * Remove leading and trailing space from the start and end of the buffer.  Intermediate
     * lines are not affected.
     * @return
     */
    public TextEditor trim() {
        text = new StringBuilder(text.toString().trim());
        return this;
    }

    /**
     * Introduce a number of spaces at the start of each line.
     * @param spaces
     * @return
     */
    public TextEditor indent(int spaces) {
        StringBuilder sb = new StringBuilder(spaces);
        for (int i = 0; i < spaces; i++) {
            sb.append(' ');
        }
        return replaceAll("^", sb.toString());
    }

    /**
     * Add a string to the end of the buffer.
     * @param s
     */
    public void append(CharSequence s) {
        text.append(s);
    }

    /**
     * Parse HTML tags, returning a Collection of HTMLToken objects.
     * @return
     */
    public Collection<HTMLToken> tokenizeHTML() {
        List<HTMLToken> tokens = new ArrayList<HTMLToken>();
        String nestedTags = nestedTagsRegex(6);

        Pattern p = Pattern.compile("" +
                "(?s:<!(--.*?--\\s*)+>)" +
                "|" +
                "(?s:<\\?.*?\\?>)" +
                "|" +
                nestedTags +
                "", Pattern.CASE_INSENSITIVE);

        Matcher m = p.matcher(text);
        int lastPos = 0;
        while (m.find()) {
            if (lastPos < m.start()) {
                tokens.add(HTMLToken.text(text.substring(lastPos, m.start())));
            }
            tokens.add(HTMLToken.tag(text.substring(m.start(), m.end())));
            lastPos = m.end();
        }
        if (lastPos < text.length()) {
            tokens.add(HTMLToken.text(text.substring(lastPos, text.length())));
        }

        return tokens;
    }

    /**
     * Regex to match a tag, possibly with nested tags such as <a href="<MTFoo>">.
     *
     * @param depth - How many levels of tags-within-tags to allow.  The example <a href="<MTFoo>"> has depth 2.
     */
    private String nestedTagsRegex(int depth) {
        if (depth == 0) {
            return "";
        } else {
            return "(?:<[a-z/!$](?:[^<>]|" + nestedTagsRegex(depth - 1) + ")*>)";
        }
    }

    /**
     * Add a string to the start of the first line of the buffer.
     * @param s
     */
    public void prepend(CharSequence s) {
        text.insert(0, s);
    }

    /**
     * Find out whether the buffer is empty.
     * @return
     */
    public boolean isEmpty() {
        return text.length() == 0;
    }
}


public class HTMLToken {
    private boolean isTag;
    private String text;

    private HTMLToken(boolean tag, String value) {
        isTag = tag;
        text = value;
    }

    public static HTMLToken tag(String text) {
        return new HTMLToken(true, text);
    }

    public static HTMLToken text(String text) {
        return new HTMLToken(false, text);
    }

    /**
     * @return <code>true</code> if this is a tag, <code>false</code> if it's text.
     */
    public boolean isTag() {
        return isTag;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        String type;
        if (isTag()) {
            type = "tag";
        } else {
            type = "text";
        }
        return type + ": " + getText();
    }
}

/*
Copyright (c) 2005, Pete Bevin.
<http://markdownj.petebevin.com>

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

* Redistributions of source code must retain the above copyright notice,
  this list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution.

* Neither the name "Markdown" nor the names of its contributors may
  be used to endorse or promote products derived from this software
  without specific prior written permission.

This software is provided by the copyright holders and contributors "as
is" and any express or implied warranties, including, but not limited
to, the implied warranties of merchantability and fitness for a
particular purpose are disclaimed. In no event shall the copyright owner
or contributors be liable for any direct, indirect, incidental, special,
exemplary, or consequential damages (including, but not limited to,
procurement of substitute goods or services; loss of use, data, or
profits; or business interruption) however caused and on any theory of
liability, whether in contract, strict liability, or tort (including
negligence or otherwise) arising in any way out of the use of this
software, even if advised of the possibility of such damage.

*/


import java.util.regex.Matcher;

public interface Replacement {
    String replacement(Matcher m);
}


/*
Copyright (c) 2005, Pete Bevin.
<http://markdownj.petebevin.com>

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

* Redistributions of source code must retain the above copyright notice,
  this list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution.

* Neither the name "Markdown" nor the names of its contributors may
  be used to endorse or promote products derived from this software
  without specific prior written permission.

This software is provided by the copyright holders and contributors "as
is" and any express or implied warranties, including, but not limited
to, the implied warranties of merchantability and fitness for a
particular purpose are disclaimed. In no event shall the copyright owner
or contributors be liable for any direct, indirect, incidental, special,
exemplary, or consequential damages (including, but not limited to,
procurement of substitute goods or services; loss of use, data, or
profits; or business interruption) however caused and on any theory of
liability, whether in contract, strict liability, or tort (including
negligence or otherwise) arising in any way out of the use of this
software, even if advised of the possibility of such damage.

*/


public class LinkDefinition {
    private String url;
    private String title;

    public LinkDefinition(String url, String title) {
        this.url = url;
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return url + " (" + title + ")";
    }
}

public class APIAddress
{
	public static final String WEBSITE_PATH = ""; //Website path, You can leave it blank in most cases.
	public static final String API_KEY = "12450"; //Application key you set in ```config.php```
	public static final String API_SECRET = "b40484df0ad979d8ba7708d24c301c38"; //Application secret you set in ```config.php```
/*
    //Debug
    public static final String DOMAIN_NAME = "http://192.168.191.1" + WEBSITE_PATH; // Main domain name
    public static final String MOBILE_DOMAIN_NAME = "http://192.168.191.1" + WEBSITE_PATH; // Domain name of mobile version
    public static final String BASIC_API_URL = "http://192.168.191.1" + WEBSITE_PATH; // Domain name of API
*/

    //Real
    public static final String DOMAIN_NAME = "https://www.94cb.com" + WEBSITE_PATH; // Main domain name
    public static final String MOBILE_DOMAIN_NAME = "https://m.94cb.com" + WEBSITE_PATH; // Domain name of mobile version
    public static final String BASIC_API_URL = "https://api.94cb.com" + WEBSITE_PATH; // Domain name of API

	//中等头像地址
	public static String MIDDLE_AVATAR_URL(String userID, String avatarSize){
		return DOMAIN_NAME + "/upload/avatar/"+ avatarSize +"/" + userID +".png";
	}

	//首页帖子列表API地址
	public static String HOME_URL(int targetPage){
		return BASIC_API_URL + "/page/"+ targetPage;
	}

	public static String TOPIC_URL(int topicID, int targetPage){
		return BASIC_API_URL + "/t/" + topicID + "-" + targetPage;
	}

	//验证码
	public static final String VERIFICATION_CODE = BASIC_API_URL + "/seccode.php";

	//登陆
	public static final String LOGIN_URL = BASIC_API_URL + "/login";

	//注册
	public static final String REGISTER_URL = BASIC_API_URL + "/register";

	//获取消息提醒
	public static final String NOTIFICATIONS_URL = BASIC_API_URL + "/notifications";

	//推送接口，维护一个长连接
	public static final String PUSH_SERVICE_URL = BASIC_API_URL + "/json/get_notifications";

	//创建新主题接口
	public static final String NEW_URL = BASIC_API_URL + "/new";

	//回复接口
	public static final String REPLY_URL = BASIC_API_URL + "/reply";
}


import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;

/**
 * Created by 灿斌 on 2015/11/7.
 */
public class CarbonWebView extends WebView {
    public CarbonWebView(Context context) {
        super(context);
    }

    public CarbonWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CarbonWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public CarbonWebView(Context context, AttributeSet attrs, int defStyle, boolean privateBrowsing) {
        super(context, attrs, defStyle, privateBrowsing);
    }

    //解决WebView盖住CardView导致CardView不响应点击事件
    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        return true;
    }

    //解决ViewPager里非首屏WebView点击事件不响应
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        //TODO: 允许长按选择文本

        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            //onScrollChanged(getScrollX(), getScrollY(), getScrollX(), getScrollY());
            return false;
        }else{
            return true;
        }
        //return super.onTouchEvent(ev);


    }
}

