package com.github.jaykkumar01.vaultspace.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.core.auth.AccountPickerHelper;
import com.github.jaykkumar01.vaultspace.core.auth.DriveConsentHelper;
import com.github.jaykkumar01.vaultspace.core.auth.GoogleUserProfileFetcher;
import com.github.jaykkumar01.vaultspace.core.auth.UserProfileConsentHelper;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.dashboard.DashboardActivity;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpace:Login";

    private UserSession userSession;

    private AccountPickerHelper accountPickerHelper;
    private DriveConsentHelper driveConsentHelper;
    private UserProfileConsentHelper profileConsentHelper;

    private String pendingEmail;

    private View loadingOverlay;

    /* ---------------- Lifecycle ---------------- */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userSession = new UserSession(this);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        initHelpers();
        initUI();
    }

    /* ---------------- Init ---------------- */

    private void initHelpers() {

        profileConsentHelper =
                new UserProfileConsentHelper(
                        this,
                        new UserProfileConsentHelper.Callback() {
                            @Override
                            public void onConsentGranted() {
                                finalizeLogin();
                            }

                            @Override
                            public void onConsentDenied() {
                                hideLoading();
                                toast("Profile permission required to continue");
                            }

                            @Override
                            public void onFailure(Exception e) {
                                hideLoading();
                                Log.e(TAG, "Profile consent check failed", e);
                                toast("Failed to verify profile permission");
                            }
                        }
                );

        driveConsentHelper =
                new DriveConsentHelper(
                        this,
                        new DriveConsentHelper.Callback() {
                            @Override
                            public void onConsentGranted() {
                                // 🔹 NEW LAYER
                                profileConsentHelper.launch(pendingEmail);
                            }

                            @Override
                            public void onConsentDenied() {
                                hideLoading();
                                toast("Drive permission required to continue");
                            }

                            @Override
                            public void onFailure(Exception e) {
                                hideLoading();
                                Log.e(TAG, "Drive consent check failed", e);
                                toast("Failed to verify Drive permission");
                            }
                        }
                );

        accountPickerHelper =
                new AccountPickerHelper(
                        this,
                        email -> {
                            Log.d(TAG, "Primary account selected: " + email);

                            pendingEmail = email;

                            showLoading();
                            driveConsentHelper.launch(email);
                        }
                );
    }


    private void initUI() {
        findViewById(R.id.btnSelectPrimaryAccount)
                .setOnClickListener(v -> accountPickerHelper.launch());
    }

    /* ---------------- Finalization ---------------- */

    private void finalizeLogin() {
        if (pendingEmail == null) {
            Log.w(TAG, "finalizeLogin() called with invalid state");
            hideLoading();
            return;
        }

        Log.d(TAG, "Finalizing login for: " + pendingEmail);

        userSession.savePrimaryAccountEmail(pendingEmail);

        GoogleUserProfileFetcher.fetch(
                getApplicationContext(),
                pendingEmail,
                profileName -> {
                    if (profileName != null) {
                        userSession.saveProfileName(profileName);
                    }
                    navigateToDashboard();
                }
        );
    }

    private void navigateToDashboard() {
        hideLoading();
        startActivity(
                new Intent(this, DashboardActivity.class)
                        .putExtra("FROM_LOGIN", true)
        );
        finish();
    }

    /* ---------------- UI Helpers ---------------- */

    private void showLoading() {
        loadingOverlay.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        loadingOverlay.setVisibility(View.GONE);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
