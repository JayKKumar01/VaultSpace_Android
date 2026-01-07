package com.github.jaykkumar01.vaultspace.login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.core.picker.AccountPickerHelper;
import com.github.jaykkumar01.vaultspace.utils.GoogleUserProfileFetcher;
import com.github.jaykkumar01.vaultspace.core.consent.PrimaryAccountConsentHelper;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.dashboard.DashboardActivity;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpace:Login";

    private UserSession userSession;

    private AccountPickerHelper accountPickerHelper;
    private PrimaryAccountConsentHelper primaryConsentHelper;

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

        primaryConsentHelper = new PrimaryAccountConsentHelper(this);

        accountPickerHelper =
                new AccountPickerHelper(
                        this,
                        email -> {
                            pendingEmail = email;
                            showLoading();

                            primaryConsentHelper.startLoginConsentFlow(
                                    email,
                                    new PrimaryAccountConsentHelper.LoginCallback() {
                                        @Override
                                        public void onAllConsentsGranted() {
                                            finalizeLogin();
                                        }

                                        @Override
                                        public void onConsentDenied() {
                                            hideLoading();
                                            toast("Required permissions not granted");
                                        }

                                        @Override
                                        public void onFailure(Exception e) {
                                            hideLoading();
                                            toast("Failed to verify permissions");
                                        }
                                    }
                            );
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
            Log.w(TAG, "finalizeLogin() called with null email");
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
