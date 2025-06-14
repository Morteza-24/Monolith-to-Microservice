package com.daquexian.chaoli.forum.model;

import java.util.List;

/**
 * Created by jianhao on 16-8-25.
 */
public class ConversationListResult {
    public List<Conversation> getResults() {
        return results;
    }

    public void setResults(List<Conversation> results) {
        this.results = results;
    }

    List<Conversation> results;
}
