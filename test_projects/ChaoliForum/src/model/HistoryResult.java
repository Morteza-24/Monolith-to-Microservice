package com.daquexian.chaoli.forum.model;

import java.util.List;

/**
 * Created by jianhao on 16-9-3.
 */
public class HistoryResult {
    public List<HistoryItem> activity;

    public List<HistoryItem> getActivity() {
        return activity;
    }

    public void setActivity(List<HistoryItem> activity) {
        this.activity = activity;
    }
}
