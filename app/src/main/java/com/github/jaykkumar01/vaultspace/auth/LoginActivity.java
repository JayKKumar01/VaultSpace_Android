package com.github.jaykkumar01.vaultspace.auth;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.core.auth.DriveConsentFlowHelper;
import com.github.jaykkumar01.vaultspace.core.auth.GoogleAccountPickerHelper;
import com.github.jaykkumar01.vaultspace.core.auth.GoogleUserProfileFetcher;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.dashboard.DashboardActivity;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpace:Login";

    private UserSession userSession;
    private GoogleAccountCredential credential;
    private DriveConsentFlowHelper consentHelper;
    private GoogleAccountPickerHelper accountPickerHelper;
    private Account pendingAccount;

    private View loadingOverlay;

    /* ---------------- Launchers ---------------- */

    private final ActivityResultLauncher<Intent> accountPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    this::onAccountPickerResult
            );

    private final ActivityResultLauncher<Intent> consentLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            Log.d(TAG, "Consent granted from UI");
                            finalizeLogin();
                        } else {
                            hideLoading();
                            Log.w(TAG, "Consent denied by user");
                            toast("Drive permission required to continue");
                        }
                    }
            );

    /* ---------------- Lifecycle ---------------- */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userSession = new UserSession(this);
        consentHelper = new DriveConsentFlowHelper();
        credential = DriveConsentFlowHelper.createCredential(this);

        loadingOverlay = findViewById(R.id.loadingOverlay);

        accountPickerHelper =
                new GoogleAccountPickerHelper(
                        accountPickerLauncher,
                        new GoogleAccountPickerHelper.Callback() {
                            @Override
                            public void onAccountPicked(Account account) {
                                Log.d(TAG, "Primary account selected: " + account.name);
                                pendingAccount = account;
                                credential.setSelectedAccount(account);
                                showLoading();
                                checkConsent();
                            }

                            @Override
                            public void onCancelled() {
                                hideLoading();
                                Log.d(TAG, "Account picker cancelled");
                            }
                        }
                );

        findViewById(R.id.btnSelectPrimaryAccount)
                .setOnClickListener(v ->
                        accountPickerLauncher.launch(
                                credential.newChooseAccountIntent()
                        )
                );
    }

    /* ---------------- Account Picker Result ---------------- */

    private void onAccountPickerResult(ActivityResult result) {
        accountPickerHelper.handleResult(
                result.getResultCode(),
                result.getData()
        );
    }

    /* ---------------- Consent Flow ---------------- */

    private void checkConsent() {
        consentHelper.checkConsent(
                credential,
                new DriveConsentFlowHelper.Callback() {
                    @Override
                    public void onConsentGranted() {
                        finalizeLogin();
                    }

                    @Override
                    public void onConsentRequired(Intent intent) {
                        consentLauncher.launch(intent);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        hideLoading();
                        Log.e(TAG, "Consent check failed", e);
                        toast("Failed to verify Drive permission");
                    }
                }
        );
    }

    /* ---------------- Finalization ---------------- */

    private void finalizeLogin() {
        Log.d(TAG, "Finalizing login for: " + pendingAccount.name);

        userSession.savePrimaryAccountEmail(pendingAccount.name);

        GoogleUserProfileFetcher.fetch(
                credential,
                profile -> {
                    if (profile != null) {
                        Log.d(TAG, "👤 Profile name: " + profile.name);
                        Log.d(TAG, "🖼 Profile photo: " + profile.picture);

                        userSession.saveProfileName(profile.name);
                        userSession.saveProfilePhoto(profile.picture);
                    }

                    navigateToDashboard();
                }
        );
    }


    private void navigateToDashboard() {
        hideLoading();
        startActivity(new Intent(this, DashboardActivity.class)
                .putExtra("FROM_LOGIN", true));
        finish();
    }

    /* ---------------- Loading Helpers ---------------- */

    private void showLoading() {
        loadingOverlay.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        loadingOverlay.setVisibility(View.GONE);
    }

    /* ---------------- UI Helpers ---------------- */

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
