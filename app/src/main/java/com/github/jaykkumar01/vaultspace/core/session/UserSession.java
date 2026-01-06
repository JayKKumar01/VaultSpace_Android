package com.github.jaykkumar01.vaultspace.core.session;

import android.content.Context;
import android.content.SharedPreferences;

public class UserSession {

    private static final String PREF_NAME = "vaultspace_session";
    private static final String KEY_PRIMARY_EMAIL = "primary_account_email";

    private final SharedPreferences prefs;

    public UserSession(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Save primary Google account email
    public void savePrimaryAccountEmail(String email) {
        prefs.edit()
                .putString(KEY_PRIMARY_EMAIL, email)
                .apply();
    }

    // Get primary account email
    public String getPrimaryAccountEmail() {
        return prefs.getString(KEY_PRIMARY_EMAIL, null);
    }

    // Check if user is logged in
    public boolean isLoggedIn() {
        return getPrimaryAccountEmail() != null;
    }

    // Clear session (for logout later)
    public void clearSession() {
        prefs.edit().clear().apply();
    }
}
