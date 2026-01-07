package com.github.jaykkumar01.vaultspace.dashboard;

import android.accounts.Account;
import android.content.Intent;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.core.auth.DriveConsentFlowHelper;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

/**
 * Handles primary account Drive consent for Dashboard.
 * Owns credential lifecycle.
 */
public class DashboardPrimaryConsentHelper {

    private static final String TAG = "VaultSpace:PrimaryConsent";

    public interface Callback {
        void onConsentGranted();
        void onConsentDenied();
    }

    private final AppCompatActivity activity;
    private final DriveConsentFlowHelper consentHelper;
    private final GoogleAccountCredential credential;
    private final String primaryEmail;

    public DashboardPrimaryConsentHelper(
            AppCompatActivity activity,
            String primaryEmail
    ) {
        this.activity = activity;
        this.primaryEmail = primaryEmail;
        this.consentHelper = new DriveConsentFlowHelper();

        // 🔥 Credential owned by helper
        this.credential =
                DriveConsentFlowHelper.createCredential(activity, true);
    }

    public void checkConsent(Callback callback) {

        Log.d(TAG, "Checking Drive consent for primary account");

        credential.setSelectedAccount(
                new Account(primaryEmail, "com.google")
        );

        consentHelper.checkConsent(
                credential,
                new DriveConsentFlowHelper.Callback() {

                    @Override
                    public void onConsentGranted() {
                        if (activity.isFinishing()) return;

                        Log.d(TAG, "Primary Drive consent granted");
                        callback.onConsentGranted();
                    }

                    @Override
                    public void onConsentRequired(Intent intent) {
                        if (activity.isFinishing()) return;

                        Log.w(TAG, "Primary Drive consent required");
                        callback.onConsentDenied();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (activity.isFinishing()) return;

                        Log.e(TAG, "Primary Drive consent failed", e);
                        callback.onConsentDenied();
                    }
                }
        );
    }
}
