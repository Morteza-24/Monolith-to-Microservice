package com.daquexian.chaoli.forum.model;

import java.util.List;

/**
 * Created by jianhao on 16-10-3.
 */

public class NotificationResult {
    public List<NotificationItem> getResults() {
        return results;
    }

    public void setResults(List<NotificationItem> results) {
        this.results = results;
    }

    List<NotificationItem> results;
}
