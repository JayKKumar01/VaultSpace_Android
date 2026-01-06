package com.github.jaykkumar01.vaultspace.dashboard;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.auth.LoginActivity;
import com.github.jaykkumar01.vaultspace.core.auth.DriveConsentFlowHelper;
import com.github.jaykkumar01.vaultspace.core.auth.GoogleAccountPickerHelper;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpace:Dashboard";
    private static final String EXTRA_FROM_LOGIN = "FROM_LOGIN";

    private DashboardSessionHelper sessionHelper;
    private DriveConsentFlowHelper consentHelper;
    private GoogleAccountCredential credential;
    private GoogleAccountPickerHelper pickerHelper;

    /* ---------------- Launchers ---------------- */

    private final ActivityResultLauncher<Intent> accountPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> pickerHelper.handleResult(
                            result.getResultCode(),
                            result.getData()
                    )
            );

    private final ActivityResultLauncher<Intent> consentLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            Log.d(TAG, "Consent granted from UI");
                            Toast.makeText(this, "Access granted ✔", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.w(TAG, "Consent denied by user");
                            Toast.makeText(this, "Access denied ❌", Toast.LENGTH_SHORT).show();
                        }
                    }
            );

    /* ---------------- Lifecycle ---------------- */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        Log.d(TAG, "onCreate");

        initCore();
        initUI();
        initTrustedAccountPicker();

        boolean fromLogin = getIntent().getBooleanExtra(EXTRA_FROM_LOGIN, false);
        checkPrimaryAccountConsentIfNeeded(fromLogin);
    }

    /* ---------------- Initialization ---------------- */

    private void initCore() {
        sessionHelper = new DashboardSessionHelper(this);
        consentHelper = new DriveConsentFlowHelper();
        credential = DriveConsentFlowHelper.createCredential(this);
    }

    private void initUI() {
        String primaryEmail = sessionHelper.getPrimaryEmail();

        if (primaryEmail == null) {
            Log.e(TAG, "Primary email missing, forcing logout");
            logout();
            return;
        }

        ((TextView) findViewById(R.id.tvUserEmail))
                .setText(primaryEmail);

        findViewById(R.id.btnAddTrustedAccount)
                .setOnClickListener(v ->
                        accountPickerLauncher.launch(
                                credential.newChooseAccountIntent()
                        )
                );

        findViewById(R.id.btnLogout)
                .setOnClickListener(v -> logout());
    }

    private void initTrustedAccountPicker() {
        pickerHelper =
                new GoogleAccountPickerHelper(
                        accountPickerLauncher,
                        new GoogleAccountPickerHelper.Callback() {
                            @Override
                            public void onAccountPicked(Account account) {
                                handleTrustedAccountPicked(account);
                            }

                            @Override
                            public void onCancelled() {
                                Log.d(TAG, "Trusted account selection cancelled");
                            }
                        }
                );
    }

    /* ---------------- Primary Account Consent ---------------- */

    private void checkPrimaryAccountConsentIfNeeded(boolean fromLogin) {
        if (fromLogin) {
            Log.d(TAG, "Dashboard opened from LoginActivity, skipping consent recheck");
            return;
        }

        String primaryEmail = sessionHelper.getPrimaryEmail();
        if (primaryEmail == null) {
            Log.e(TAG, "Primary email missing during consent check");
            logout();
            return;
        }

        Log.d(TAG, "Re-checking Drive consent for primary account");

        Account primaryAccount = new Account(primaryEmail, "com.google");
        credential.setSelectedAccount(primaryAccount);

        runConsentCheck(true);
    }

    /* ---------------- Trusted Account Flow ---------------- */

    private void handleTrustedAccountPicked(Account account) {
        String primaryEmail = sessionHelper.getPrimaryEmail();

        Log.d(TAG, "Trusted account selected: " + account.name);
        Log.d(TAG, "Primary account: " + primaryEmail);

        if (account.name.equalsIgnoreCase(primaryEmail)) {
            Log.w(TAG, "Primary account selected as trusted account");
            Toast.makeText(
                    this,
                    "Primary account cannot be added again",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        credential.setSelectedAccount(account);
        runConsentCheck(false);
    }

    /* ---------------- Unified Consent Check ---------------- */

    private void runConsentCheck(boolean isPrimary) {
        consentHelper.checkConsent(
                this,
                credential,
                new DriveConsentFlowHelper.Callback() {
                    @Override
                    public void onConsentGranted() {
                        Log.d(TAG,
                                (isPrimary ? "Primary" : "Trusted")
                                        + " account consent already granted"
                        );

                        if (!isPrimary) {
                            Toast.makeText(
                                    DashboardActivity.this,
                                    "Access granted ✔",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onConsentRequired(Intent intent) {
                        Log.d(TAG,
                                (isPrimary ? "Primary" : "Trusted")
                                        + " account consent required"
                        );
                        consentLauncher.launch(intent);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG,
                                (isPrimary ? "Primary" : "Trusted")
                                        + " account consent check failed",
                                e
                        );

                        if (isPrimary) {
                            Toast.makeText(
                                    DashboardActivity.this,
                                    "Drive access required. Please login again.",
                                    Toast.LENGTH_LONG
                            ).show();
                            logout();
                        } else {
                            Toast.makeText(
                                    DashboardActivity.this,
                                    "Consent check failed",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
                }
        );
    }

    /* ---------------- Logout ---------------- */

    private void logout() {
        Log.d(TAG, "Logging out");
        sessionHelper.logout();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
