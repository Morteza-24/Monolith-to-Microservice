package com.daquexian.chaoli.forum.model;

/**
 * Created by daquexian on 17-1-21.
 */

public class UserIdAndTokenResult {
    private int userId;
    private String token;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
