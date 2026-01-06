package com.github.jaykkumar01.vaultspace.dashboard;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.auth.LoginActivity;
import com.github.jaykkumar01.vaultspace.core.drive.DriveConsentUtil;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import java.util.Collections;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpaceDashboard";

    private UserSession userSession;
    private boolean fromLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        Log.d(TAG, "onCreate");

        userSession = new UserSession(this);
        fromLogin = getIntent().getBooleanExtra("FROM_LOGIN", false);

        String email = userSession.getPrimaryAccountEmail();
        ((TextView) findViewById(R.id.tvUserEmail)).setText(email);

        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        if (!userSession.isLoggedIn()) {
            logout();
            return;
        }

        if (fromLogin) {
            Log.d(TAG, "Opened from login, skipping consent check");
            fromLogin = false;
            return;
        }

        Log.d(TAG, "Checking Drive consent on resume");

        GoogleAccountCredential credential =
                GoogleAccountCredential.usingOAuth2(
                        this,
                        Collections.singleton("https://www.googleapis.com/auth/drive.file")
                );

        credential.setSelectedAccount(
                new Account(userSession.getPrimaryAccountEmail(), "com.google")
        );

        DriveConsentUtil.checkConsent(
                this,
                credential,
                new DriveConsentUtil.ConsentCallback() {
                    @Override
                    public void onGranted() {
                        Log.d(TAG, "Consent still valid");
                    }

                    @Override
                    public void onRecoverable(
                            com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException e
                    ) {
                        Log.w(TAG, "Consent revoked, clearing session");
                        logout();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Consent check failed", e);
                        logout();
                    }
                }
        );
    }

    private void logout() {
        Log.d(TAG, "Logging out");
        userSession.clearSession();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
