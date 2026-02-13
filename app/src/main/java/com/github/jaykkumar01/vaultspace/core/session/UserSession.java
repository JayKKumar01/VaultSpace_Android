package com.github.jaykkumar01.vaultspace.core.session;

import android.content.Context;
import android.content.SharedPreferences;

import com.github.jaykkumar01.vaultspace.core.drive.AlbumMediaRepository;
import com.github.jaykkumar01.vaultspace.core.drive.AlbumsRepository;
import com.github.jaykkumar01.vaultspace.core.drive.DriveFolderRepository;
import com.github.jaykkumar01.vaultspace.core.drive.TrustedAccountsRepository;
import com.github.jaykkumar01.vaultspace.core.session.cache.AppCacheManager;
import com.github.jaykkumar01.vaultspace.core.session.db.SessionStoreRegistry;
import com.github.jaykkumar01.vaultspace.core.upload.UploadOrchestrator;
public class UserSession {

    private static final String PREF_NAME = "vaultspace_session";

    private static final String KEY_PRIMARY_EMAIL = "primary_account_email";
    private static final String KEY_PROFILE_NAME = "profile_name";

    private final SharedPreferences prefs;
    private final Context appContext;

    private static VaultSessionCache vaultCache;
    private final SessionStoreRegistry storeRegistry;

    public UserSession(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.storeRegistry = new SessionStoreRegistry(appContext);
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
        if (vaultCache == null) vaultCache = new VaultSessionCache();
        return vaultCache;
    }

    /* ---------------- Session Stores ---------------- */

    public UploadRetryStore getUploadRetryStore() {
        return storeRegistry.get(UploadRetryStore.class);
    }

    public SetupIgnoreStore getSetupIgnoreStore() {
        return storeRegistry.get(SetupIgnoreStore.class);
    }

    /* ---------------- Session ---------------- */

    public boolean isLoggedIn() {
        return getPrimaryAccountEmail() != null;
    }

    public void clearSession() {
        prefs.edit().clear().apply();

        storeRegistry.clearAll();

        if (vaultCache != null) {
            vaultCache.clear();
            vaultCache = null;
        }

        PrimaryUserCoordinator.clearSavedProfilePhoto(appContext);
        UploadOrchestrator.getInstance(appContext).onSessionCleared();
        DriveFolderRepository.onSessionCleared();
        TrustedAccountsRepository.destroy();
        AlbumsRepository.destroy();
        AlbumMediaRepository.destroy();
        AppCacheManager.clearCache(appContext);
    }
}
