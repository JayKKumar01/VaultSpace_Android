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

    private String primaryEmail;
    private Account pendingTrustedAccount;

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
                        if (isFinishing()) return;

                        if (result.getResultCode() == RESULT_OK) {
                            Log.d(TAG, "Consent granted from UI, resuming flow");
                            runConsentCheck(false); // ✅ resume trusted flow
                        } else {
                            Log.w(TAG, "Consent denied by user");
                            pendingTrustedAccount = null;
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
        initSession();
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

    private void initSession() {
        primaryEmail = sessionHelper.getPrimaryEmail();
        if (primaryEmail == null) {
            Log.e(TAG, "Primary email missing");
            forceLogout("Session expired. Please login again.");
        }
    }

    private void initUI() {
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
        if (fromLogin) return;

        credential.setSelectedAccount(
                new Account(primaryEmail, "com.google")
        );

        runConsentCheck(true);
    }

    /* ---------------- Trusted Account Flow ---------------- */

    private void handleTrustedAccountPicked(Account account) {
        Log.d(TAG, "Trusted account selected: " + account.name);

        if (account.name.equalsIgnoreCase(primaryEmail)) {
            Toast.makeText(this,
                    "Primary account cannot be added again",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        pendingTrustedAccount = account;
        credential.setSelectedAccount(account);
        runConsentCheck(false);
    }

    /* ---------------- Unified Consent Check ---------------- */

    private void runConsentCheck(boolean isPrimary) {
        consentHelper.checkConsent(
                credential,
                new DriveConsentFlowHelper.Callback() {

                    @Override
                    public void onConsentGranted() {
                        if (isFinishing()) return;

                        if (!isPrimary && pendingTrustedAccount != null) {

                            Account trusted = pendingTrustedAccount;
                            pendingTrustedAccount = null;

                            Toast.makeText(
                                    DashboardActivity.this,
                                    "Access granted ✔",
                                    Toast.LENGTH_SHORT
                            ).show();

                            new Thread(() -> {
                                try {
                                    TrustedAccountsDriveHelper helper =
                                            new TrustedAccountsDriveHelper(
                                                    DashboardActivity.this,
                                                    primaryEmail
                                            );

                                    helper.addTrustedAccount(trusted.name);

                                    Log.d(TAG, "Trusted account added to Drive");

                                } catch (Exception e) {
                                    Log.e(TAG, "Failed to add trusted account", e);
                                }
                            }).start();
                        }
                    }

                    @Override
                    public void onConsentRequired(Intent intent) {
                        if (isFinishing()) return;

                        if (isPrimary) {
                            forceLogout("Drive access required. Please login again.");
                        } else {
                            consentLauncher.launch(intent);
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (isFinishing()) return;

                        pendingTrustedAccount = null;

                        Log.e(TAG, "Consent check failed", e);

                        if (isPrimary) {
                            forceLogout("Drive access required. Please login again.");
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
