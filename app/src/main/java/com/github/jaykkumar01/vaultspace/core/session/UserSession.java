package com.github.jaykkumar01.vaultspace.core.session;

public class UserSession {

    private static UserSession instance;
    private boolean loggedIn = false;

    private UserSession() {
    }

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    // TEMP: always false for now
    public boolean isLoggedIn() {
        return loggedIn;
    }

    // For future use
    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }
}
