package com.github.jaykkumar01.vaultspace.dashboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;

import com.github.jaykkumar01.vaultspace.core.auth.GoogleUserProfileFetcher;
import com.github.jaykkumar01.vaultspace.views.ProfileInfoView;

/**
 * Handles profile header binding logic for Dashboard.
 * Keeps DashboardActivity clean and focused.
 */
public class DashboardProfileInfoHelper {

    private final Context context;
    private final ProfileInfoView profileInfoView;

    public DashboardProfileInfoHelper(
            Context context,
            ProfileInfoView profileInfoView
    ) {
        this.context = context.getApplicationContext();
        this.profileInfoView = profileInfoView;
    }

    /**
     * Bind profile info to ProfileInfoView.
     *
     * @param primaryEmail Primary Google account email (non-null)
     * @param profileName  Cached profile name (nullable)
     */
    public void bindProfile(String primaryEmail, String profileName) {

        // ---- Resolve display name & email ----
        String displayName;
        String displayEmail;

        if (!TextUtils.isEmpty(profileName)) {
            displayName = profileName;
            displayEmail = primaryEmail;
        } else {
            displayName = primaryEmail;
            displayEmail = "";
        }

        // ---- Load cached profile photo ----
        Bitmap profileBitmap =
                GoogleUserProfileFetcher.loadSavedProfilePhoto(context);

        // ---- Bind to custom view ----
        profileInfoView.setProfile(
                profileBitmap,
                displayName,
                displayEmail
        );
    }
}
