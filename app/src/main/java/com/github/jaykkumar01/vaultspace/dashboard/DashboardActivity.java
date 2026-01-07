package com.github.jaykkumar01.vaultspace.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.core.consent.PrimaryAccountConsentHelper;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.login.LoginActivity;
import com.github.jaykkumar01.vaultspace.views.ProfileInfoView;
import com.github.jaykkumar01.vaultspace.views.StorageBarView;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpace:Dashboard";
    private static final String EXTRA_FROM_LOGIN = "FROM_LOGIN";

    private UserSession userSession;

    private String primaryEmail;
    private String profileName;

    private DashboardStorageBarHelper storageBarHelper;
    private PrimaryAccountConsentHelper primaryAccountConsentHelper;

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
        userSession = new UserSession(this);

        primaryEmail = userSession.getPrimaryAccountEmail();
        profileName = userSession.getProfileName();

        Log.d(TAG, "Primary email = " + primaryEmail);
        Log.d(TAG, "Profile name = " + profileName);

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

        findViewById(R.id.btnExpandVault)
                .setOnClickListener(v -> { /* future trusted account flow */ });

        findViewById(R.id.btnLogout)
                .setOnClickListener(v -> logout());
    }

    private void initHelpers() {
        primaryAccountConsentHelper = new PrimaryAccountConsentHelper(this);
    }

    /* ---------------- Consent Flow ---------------- */

    private void handleConsent(boolean fromLogin) {
        if (fromLogin) {
            Log.d(TAG, "Opened from Login — skipping consent check");
            storageBarHelper.loadAndBindStorage();
            return;
        }

        primaryAccountConsentHelper.checkConsentsSilently(primaryEmail, result -> {
            switch (result) {
                case GRANTED:
                    storageBarHelper.loadAndBindStorage();
                    break;

                case RECOVERABLE:
                case FAILED:
                    forceLogout("Permissions were revoked. Please login again.");
                    break;
            }
        });
    }

    /* ---------------- Logout ---------------- */

    private void forceLogout(String reason) {
        Toast.makeText(this, reason, Toast.LENGTH_LONG).show();
        logout();
    }

    private void logout() {
        Log.d(TAG, "Clearing session");
        userSession.clearSession();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
