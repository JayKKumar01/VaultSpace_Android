package com.github.jaykkumar01.vaultspace.dashboard.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;

import com.github.jaykkumar01.vaultspace.utils.GoogleUserProfileFetcher;
import com.github.jaykkumar01.vaultspace.views.creative.ProfileInfoView;

/**
 * Handles profile header binding logic for Dashboard.
 */
public class DashboardProfileInfoHelper {

    public interface ProfileNameCallback {
        void onProfileNameAvailable(String profileName);
    }

    private final Context context;
    private final ProfileInfoView profileInfoView;
    private final String primaryEmail;

    public DashboardProfileInfoHelper(
            Context context,
            ProfileInfoView profileInfoView,
            String primaryEmail
    ) {
        this.context = context.getApplicationContext();
        this.profileInfoView = profileInfoView;
        this.primaryEmail = primaryEmail;
    }

    /**
     * Bind profile using already-known name.
     */
    public void bindProfile(String profileName) {
        bindInternal(profileName);
    }

    /**
     * Fetch latest profile name, bind UI,
     * and notify Dashboard via callback.
     */
    public void bindProfileLatest(ProfileNameCallback callback) {

        GoogleUserProfileFetcher.fetch(
                context,
                primaryEmail,
                fetchedName -> {
                    bindInternal(fetchedName);

                    if (callback != null && !TextUtils.isEmpty(fetchedName)) {
                        callback.onProfileNameAvailable(fetchedName);
                    }
                }
        );
    }

    /* ---------------- Internal ---------------- */

    private void bindInternal(String profileName) {

        String displayName;
        String displayEmail;

        if (!TextUtils.isEmpty(profileName)) {
            displayName = profileName;
            displayEmail = primaryEmail;
        } else {
            displayName = primaryEmail;
            displayEmail = "";
        }

        Bitmap profileBitmap =
                GoogleUserProfileFetcher.loadSavedProfilePhoto(context);

//        profileInfoView.setProfile(
//                profileBitmap,
//                displayName,
//                displayEmail
//        );
    }
}
