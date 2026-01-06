package com.github.jaykkumar01.vaultspace.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.core.drive.DriveConsentUtil;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.dashboard.DashboardActivity;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import java.util.Collections;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpaceLogin";

    private GoogleAccountCredential credential;
    private UserSession userSession;
    private String pendingEmail;

    private final ActivityResultLauncher<Intent> accountPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {

                        Log.d(TAG, "Account picker returned");

                        if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                            Log.w(TAG, "Account selection cancelled");
                            return;
                        }

                        String email =
                                result.getData()
                                        .getStringExtra(AccountManager.KEY_ACCOUNT_NAME);

                        if (email == null) {
                            Log.e(TAG, "Invalid account");
                            return;
                        }

                        Log.d(TAG, "Account selected: " + email);
                        pendingEmail = email;

                        credential.setSelectedAccount(
                                new Account(email, "com.google")
                        );

                        checkConsent();
                    }
            );

    private final ActivityResultLauncher<Intent> consentLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            Log.d(TAG, "Consent granted from UI");
                            finalizeLogin();
                        } else {
                            Log.w(TAG, "Consent denied");
                            toast("Drive permission required");
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Log.d(TAG, "onCreate");

        userSession = new UserSession(this);

        credential =
                GoogleAccountCredential.usingOAuth2(
                        this,
                        Collections.singleton("https://www.googleapis.com/auth/drive.file")
                );

        findViewById(R.id.btnSelectPrimaryAccount)
                .setOnClickListener(v -> accountPickerLauncher.launch(
                        credential.newChooseAccountIntent()
                ));
    }

    private void checkConsent() {
        DriveConsentUtil.checkConsent(
                this,
                credential,
                new DriveConsentUtil.ConsentCallback() {
                    @Override
                    public void onGranted() {
                        runOnUiThread(LoginActivity.this::finalizeLogin);
                    }

                    @Override
                    public void onRecoverable(
                            com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException e
                    ) {
                        runOnUiThread(() -> consentLauncher.launch(e.getIntent()));
                    }

                    @Override
                    public void onFailure(Exception e) {
                        toast("Consent check failed");
                    }
                }
        );
    }

    private void finalizeLogin() {
        Log.d(TAG, "Finalizing login");

        userSession.savePrimaryAccountEmail(pendingEmail);

        Intent i = new Intent(this, DashboardActivity.class);
        i.putExtra("FROM_LOGIN", true);
        startActivity(i);
        finish();
    }

    private void toast(String msg) {
        runOnUiThread(() ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        );
    }
}
