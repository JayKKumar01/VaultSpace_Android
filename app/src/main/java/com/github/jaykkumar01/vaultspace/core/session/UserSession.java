package com.github.jaykkumar01.vaultspace.core.session;

import android.content.Context;
import android.content.SharedPreferences;

import com.github.jaykkumar01.vaultspace.utils.GoogleUserProfileFetcher;

public class UserSession {

    private static final String PREF_NAME = "vaultspace_session";

    private static final String KEY_PRIMARY_EMAIL = "primary_account_email";
    private static final String KEY_PROFILE_NAME = "profile_name";

    private final SharedPreferences prefs;
    private final Context appContext;

    public UserSession(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );
    }

    /* ---------------- Primary Account ---------------- */

    public void savePrimaryAccountEmail(String email) {
        prefs.edit()
                .putString(KEY_PRIMARY_EMAIL, email)
                .apply();
    }

    public String getPrimaryAccountEmail() {
        return prefs.getString(KEY_PRIMARY_EMAIL, null);
    }

    /* ---------------- Profile Info ---------------- */

    public void saveProfileName(String name) {
        prefs.edit()
                .putString(KEY_PROFILE_NAME, name)
                .apply();
    }

    public String getProfileName() {
        return prefs.getString(KEY_PROFILE_NAME, null);
    }

    /* ---------------- Session ---------------- */

    public boolean isLoggedIn() {
        return getPrimaryAccountEmail() != null;
    }

    /**
     * Clears logical session state and delegates
     * physical cleanup to owning components.
     */
    public void clearSession() {
        // Clear session data
        prefs.edit().clear().apply();

        // Delegate profile photo cleanup
        GoogleUserProfileFetcher.clearSavedProfilePhoto(appContext);
    }
}
