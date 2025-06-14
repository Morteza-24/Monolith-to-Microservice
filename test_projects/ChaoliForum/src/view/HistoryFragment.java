package com.daquexian.chaoli.forum.view;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.databinding.Observable;
import android.databinding.ObservableBoolean;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.data.Me;
import com.daquexian.chaoli.forum.databinding.HomepageHistoryBinding;
import com.daquexian.chaoli.forum.viewmodel.BaseViewModel;
import com.daquexian.chaoli.forum.viewmodel.HistoryFragmentVM;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

/**
 * Created by jianhao on 16-6-5.
 */

public class HistoryFragment extends Fragment implements IView, SwipyRefreshLayout.OnRefreshListener {
    //String mUsername, mAvatarSuffix;
    //int mUserId;
    SwipyRefreshLayout mSwipyRefreshLayout;
    Boolean bottom = true;
    Context activityContext;
    int type;

    private static final String TAG = "HistoryFragment";
    private HistoryFragmentVM viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        type = arguments.getInt("type");
        if (type == HistoryFragmentVM.TYPE_ACTIVITY) {
            viewModel = new HistoryFragmentVM(type,
                    arguments.getInt("userId"),
                    arguments.getString("username"),
                    arguments.getString("avatarSuffix"));
        } else if (type == HistoryFragmentVM.TYPE_NOTIFICATION) {
            viewModel = new HistoryFragmentVM(type, Me.getMyUserId(), Me.getMyUsername(), Me.getMyAvatarSuffix());
        } else {
            throw new RuntimeException("type can only be 0 or 1");
        }
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mSwipyRefreshLayout = (SwipyRefreshLayout) inflater.inflate(R.layout.homepage_history, container, false);

        mSwipyRefreshLayout.setDirection(type == HistoryFragmentVM.TYPE_ACTIVITY ? SwipyRefreshLayoutDirection.BOTH : SwipyRefreshLayoutDirection.TOP);
        mSwipyRefreshLayout.setOnRefreshListener(this);

        setViewModel(viewModel);

        onRefresh(SwipyRefreshLayoutDirection.TOP);

        RecyclerView rvHistory = (RecyclerView) mSwipyRefreshLayout.findViewById(R.id.rvHomepageItems);
        rvHistory.setLayoutManager(new LinearLayoutManager(activityContext));
        //rvHistory.setAdapter(myAdapter);

        rvHistory.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                //得到当前显示的最后一个item的view
                View lastChildView = recyclerView.getLayoutManager().getChildAt(recyclerView.getLayoutManager().getChildCount()-1);
                //得到lastChildView的bottom坐标值
                int lastChildBottom = lastChildView.getBottom();
                //得到Recyclerview的底部坐标减去底部padding值，也就是显示内容最底部的坐标
                int recyclerBottom =  recyclerView.getBottom()-recyclerView.getPaddingBottom();
                //通过这个lastChildView得到这个view当前的position值
                int lastVisiblePosition  = recyclerView.getLayoutManager().getPosition(lastChildView);

                //判断lastChildView的bottom值跟recyclerBottom
                //判断lastPosition是不是最后一个position
                //如果两个条件都满足则说明是真正的滑动到了底部
				/* Why <= ? */
                int lastPosition = recyclerView.getLayoutManager().getItemCount() - 1;

                if(lastChildBottom == recyclerBottom && lastPosition == recyclerView.getLayoutManager().getItemCount()-1 ){
                    bottom = true;
                    mSwipyRefreshLayout.setEnabled(true);
                }else{
                    bottom = false;
                }

                if (lastVisiblePosition >= lastPosition - 3) viewModel.tryToLoadMore();
            }
        });

        viewModel.showProgressDialog.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                if (((ObservableBoolean) observable).get()) {
                    ((HomepageActivity) activityContext).showProcessDialog(ChaoliApplication.getAppContext().getString(R.string.just_a_sec));
                } else {
                    ((HomepageActivity) activityContext).dismissProcessDialog();
                }
            }
        });

        viewModel.goToPost.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                Intent intent = new Intent(activityContext, PostActivity.class);
                intent.putExtra("conversationId", viewModel.intendedConversationId.get());
                intent.putExtra("conversationTitle", viewModel.intendedConversationTitle.get());
                if (viewModel.intendedConversationPage.get() != -1) intent.putExtra("page", viewModel.intendedConversationPage.get());
                startActivity(intent);
            }
        });

        return mSwipyRefreshLayout;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activityContext = context;
    }

    @Override
    public void onRefresh(SwipyRefreshLayoutDirection direction) {
        if (direction == SwipyRefreshLayoutDirection.TOP) {
            viewModel.refresh();
        } else {
            viewModel.loadMore();
        }
    }

    public void setRefreshEnabled(Boolean enabled){
        mSwipyRefreshLayout.setEnabled(enabled || bottom);
    }

    @Override
    public void setViewModel(BaseViewModel viewModel) {
        this.viewModel = (HistoryFragmentVM) viewModel;
        //this.viewModel.setUrl(Constants.GET_ACTIVITIES_URL + mUserId);
        HomepageHistoryBinding binding = DataBindingUtil.bind(mSwipyRefreshLayout);
        binding.setViewModel(this.viewModel);
    }
}
