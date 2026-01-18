package com.github.jaykkumar01.vaultspace.core.session;

import android.content.Context;
import android.content.SharedPreferences;

import com.github.jaykkumar01.vaultspace.core.upload.UploadOrchestrator;

public class UserSession {

    private static final String PREF_NAME = "vaultspace_session";

    private static final String KEY_PRIMARY_EMAIL = "primary_account_email";
    private static final String KEY_PROFILE_NAME = "profile_name";

    private final SharedPreferences prefs;
    private final Context appContext;

    // Session-scoped cache holder
    private static VaultSessionCache vaultCache;

    // Session-scoped retry store (persisted)
    private UploadRetryStore uploadRetryStore;
    private UploadFailureStore uploadFailureStore;




    public UserSession(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );
    }

    /* ---------------- Primary Account ---------------- */

    public void savePrimaryAccountEmail(String email) {
        prefs.edit().putString(KEY_PRIMARY_EMAIL, email).apply();
    }

    public String getPrimaryAccountEmail() {
        return prefs.getString(KEY_PRIMARY_EMAIL, null);
    }

    /* ---------------- Profile Info ---------------- */

    public void saveProfileName(String name) {
        prefs.edit().putString(KEY_PROFILE_NAME, name).apply();
    }

    public String getProfileName() {
        return prefs.getString(KEY_PROFILE_NAME, null);
    }

    /* ---------------- Vault Cache ---------------- */

    public VaultSessionCache getVaultCache() {
        if (vaultCache == null) {
            vaultCache = new VaultSessionCache();
        }
        return vaultCache;
    }

    /* ---------------- Upload Retry Store ---------------- */

    public UploadRetryStore getUploadRetryStore() {
        if (uploadRetryStore == null) {
            uploadRetryStore = new UploadRetryStore(appContext);
        }
        return uploadRetryStore;
    }

    public UploadFailureStore getUploadFailureStore() {
        if (uploadFailureStore == null) {
            uploadFailureStore = new UploadFailureStore(appContext);
        }
        return uploadFailureStore;
    }


    /* ---------------- Session ---------------- */

    public boolean isLoggedIn() {
        return getPrimaryAccountEmail() != null;
    }

    public void clearSession() {
        // clear persisted session data
        prefs.edit().clear().apply();

        // clear retry store
        if (uploadRetryStore != null) {
            uploadRetryStore.onSessionCleared();
            uploadRetryStore = null;
        }

        if (uploadFailureStore != null) {
            uploadFailureStore.onSessionCleared();
            uploadFailureStore = null;
        }


        // clear in-memory vault cache
        if (vaultCache != null) {
            vaultCache.clear();
            vaultCache = null;
        }

        PrimaryUserCoordinator.clearSavedProfilePhoto(appContext);
        UploadOrchestrator.getInstance(appContext).onSessionCleared();
    }
}
