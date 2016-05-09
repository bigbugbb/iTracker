package com.localytics.android.itracker.data.model;

public class User {
    public String id;
    public String email;
    public String username;
    public String password;
    public String password_confirmation;
    public String auth_token;

    public User(String email, String username, String password, String passwordConfirmation) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.password_confirmation = passwordConfirmation;
    }
}