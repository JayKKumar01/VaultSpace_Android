package com.github.jaykkumar01.vaultspace.core.session;

import android.content.Context;
import android.content.SharedPreferences;

public class UserSession {

    private static final String PREF_NAME = "vaultspace_session";

    private static final String KEY_PRIMARY_EMAIL = "primary_account_email";
    private static final String KEY_PROFILE_NAME = "profile_name";
    private static final String KEY_PROFILE_PHOTO = "profile_photo";

    private final SharedPreferences prefs;

    public UserSession(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
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

    public void saveProfilePhoto(String photoUrl) {
        prefs.edit()
                .putString(KEY_PROFILE_PHOTO, photoUrl)
                .apply();
    }

    public String getProfilePhoto() {
        return prefs.getString(KEY_PROFILE_PHOTO, null);
    }

    /* ---------------- Session ---------------- */

    public boolean isLoggedIn() {
        return getPrimaryAccountEmail() != null;
    }

    public void clearSession() {
        prefs.edit().clear().apply();
    }
}
