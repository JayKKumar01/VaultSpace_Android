package com.github.jaykkumar01.vaultspace.core.session;

import android.content.Context;
import android.content.SharedPreferences;

public class UserSession {
    private static UserSession instance;
    private SharedPreferences sharedPreferences;

    private static final String PREF_NAME = "UserSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_AUTH_TOKEN = "authToken";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_AUTH_PROVIDER = "authProvider"; // "google" or "email"
    private static final String KEY_REFRESH_TOKEN = "refreshToken"; // For token refresh if needed

    private UserSession(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized UserSession getInstance(Context context) {
        if (instance == null) {
            instance = new UserSession(context);
        }
        return instance;
    }

    public static synchronized UserSession getInstance() {
        if (instance == null) {
            throw new IllegalStateException("UserSession not initialized. Call getInstance(Context) first.");
        }
        return instance;
    }

    // For Google login
    public void setGoogleLogin(String idToken, String email) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_AUTH_TOKEN, idToken);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_AUTH_PROVIDER, "google");
        // Generate a persistent token for your backend (you might want to send idToken to your server and get a custom token)
        editor.putString(KEY_USER_ID, generateUserIdFromEmail(email));
        editor.apply();
    }

    // For Email/Password login
    public void setEmailPasswordLogin(String authToken, String email, String userId, String refreshToken) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_AUTH_TOKEN, authToken);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_AUTH_PROVIDER, "email");
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getAuthToken() {
        return sharedPreferences.getString(KEY_AUTH_TOKEN, null);
    }

    public String getUserEmail() {
        return sharedPreferences.getString(KEY_USER_EMAIL, null);
    }

    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID, null);
    }

    public String getAuthProvider() {
        return sharedPreferences.getString(KEY_AUTH_PROVIDER, null);
    }

    public String getRefreshToken() {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null);
    }

    public void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    private String generateUserIdFromEmail(String email) {
        // Simple hash for demo. In production, get user ID from your backend
        return String.valueOf(Math.abs(email.hashCode()));
    }

    // Update token if you get a new one (for refresh scenarios)
    public void updateAuthToken(String newToken) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_AUTH_TOKEN, newToken);
        editor.apply();
    }
}