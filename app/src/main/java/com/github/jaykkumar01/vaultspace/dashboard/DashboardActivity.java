package com.github.jaykkumar01.vaultspace.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.auth.LoginActivity;
import com.github.jaykkumar01.vaultspace.views.ProfileInfoView;
import com.github.jaykkumar01.vaultspace.views.StorageBarView;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpace:Dashboard";
    private static final String EXTRA_FROM_LOGIN = "FROM_LOGIN";

    private DashboardSessionHelper sessionHelper;

    private String primaryEmail;
    private String profileName;

    private DashboardStorageBarHelper storageBarHelper;
    private DashboardPrimaryConsentHelper consentHelper;

    /* ---------------- Lifecycle ---------------- */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        Log.d(TAG, "onCreate()");

        initSession();
        initUI();
        initHelpers();

        boolean fromLogin = getIntent().getBooleanExtra(EXTRA_FROM_LOGIN, false);
        handleConsent(fromLogin);
    }

    /* ---------------- Init ---------------- */

    private void initSession() {
        sessionHelper = new DashboardSessionHelper(this);

        primaryEmail = sessionHelper.getPrimaryEmail();
        profileName = sessionHelper.getProfileName();

        if (primaryEmail == null) {
            Log.e(TAG, "Primary email missing");
            forceLogout("Session expired. Please login again.");
        }
    }

    private void initUI() {
        ProfileInfoView profileInfoView = findViewById(R.id.profileInfo);
        new DashboardProfileInfoHelper(this, profileInfoView)
                .bindProfile(primaryEmail, profileName);

        StorageBarView storageBar = findViewById(R.id.storageBar);
        storageBarHelper =
                new DashboardStorageBarHelper(this, storageBar, primaryEmail);

        findViewById(R.id.btnLogout)
                .setOnClickListener(v -> logout());
    }

    private void initHelpers() {
        consentHelper = new DashboardPrimaryConsentHelper(this, primaryEmail);
    }

    /* ---------------- Consent Flow ---------------- */

    private void handleConsent(boolean fromLogin) {
        if (fromLogin) {
            Log.d(TAG, "Opened from Login — skipping consent check");
            storageBarHelper.loadAndBindStorage();
            return;
        }

        consentHelper.checkConsent(new DashboardPrimaryConsentHelper.Callback() {
            @Override
            public void onConsentGranted() {
                storageBarHelper.loadAndBindStorage();
            }

            @Override
            public void onConsentDenied() {
                forceLogout("Drive access required. Please login again.");
            }
        });
    }

    /* ---------------- Logout ---------------- */

    private void forceLogout(String reason) {
        Toast.makeText(this, reason, Toast.LENGTH_LONG).show();
        logout();
    }

    private void logout() {
        sessionHelper.logout();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
