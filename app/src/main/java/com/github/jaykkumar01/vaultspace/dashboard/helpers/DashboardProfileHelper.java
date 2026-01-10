package com.github.jaykkumar01.vaultspace.dashboard.helpers;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.core.session.PrimaryUserCoordinator;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.views.creative.ProfileInfoView;

public final class DashboardProfileHelper {

    private static final String TAG = "VaultSpace:DashboardProfile";

    private final AppCompatActivity activity;
    private final UserSession session;
    private final ProfileInfoView profileView;

    private String lastKnownName;

    public DashboardProfileHelper(@NonNull AppCompatActivity activity) {
        this.activity = activity;
        this.session = new UserSession(activity);

        this.profileView =
                activity.findViewById(R.id.profileInfo);

        if (profileView == null) {
            throw new IllegalStateException(
                    "ProfileInfoView (R.id.profileInfo) not found in Dashboard layout"
            );
        }
    }

    /* ==========================================================
     * Entry
     * ========================================================== */

    public void attach() {
        bindCachedProfile();
        refreshProfileSilently();
    }

    /* ==========================================================
     * Phase 1 — Instant bind (sync)
     * ========================================================== */

    private void bindCachedProfile() {
        String email = session.getPrimaryAccountEmail();
        String name = session.getProfileName();
        Bitmap photo =
                PrimaryUserCoordinator.loadProfilePhoto(activity);

        lastKnownName = name;

        if (email != null) {
            profileView.setEmail(email);
        }

        if (name != null) {
            profileView.setName(name);
        }

        profileView.setProfileImage(photo);
    }

    /* ==========================================================
     * Phase 2 — Silent refresh (async)
     * ========================================================== */

    private void refreshProfileSilently() {
        PrimaryUserCoordinator.refresh(
                activity,
                new PrimaryUserCoordinator.Callback() {
                    @Override
                    public void onSuccess() {
                        applyRefreshedProfile();
                    }

                    @Override
                    public void onError() {
                        Log.d(TAG, "Profile refresh skipped (non-fatal)");
                    }
                }
        );
    }

    private void applyRefreshedProfile() {
        String newName = session.getProfileName();

        if (newName != null && !newName.equals(lastKnownName)) {
            lastKnownName = newName;
            profileView.setName(newName);
        }

        Bitmap newPhoto =
                PrimaryUserCoordinator.loadProfilePhoto(activity);

        if (newPhoto != null) {
            profileView.setProfileImage(newPhoto);
        }
    }
}
