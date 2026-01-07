package com.github.jaykkumar01.vaultspace.dashboard;

import android.accounts.Account;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
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
import com.github.jaykkumar01.vaultspace.core.auth.GoogleUserProfileFetcher;
import com.github.jaykkumar01.vaultspace.models.TrustedAccount;
import com.github.jaykkumar01.vaultspace.utils.Base;
import com.github.jaykkumar01.vaultspace.views.ProfileInfoView;
import com.github.jaykkumar01.vaultspace.views.StorageBarView;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpace:Dashboard";
    private static final String EXTRA_FROM_LOGIN = "FROM_LOGIN";

    private DashboardSessionHelper sessionHelper;
    private DriveConsentFlowHelper consentHelper;
    private GoogleAccountCredential credential;
    private GoogleAccountPickerHelper pickerHelper;

    private String primaryEmail;
    private String profileName;
    private Account pendingTrustedAccount;

    private View loadingOverlay;

    private ProfileInfoView profileInfoView;
    private StorageBarView storageBar;


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
                            Log.d(TAG, "Consent UI approved, resuming trusted flow");
                            runConsentCheck(false);
                        } else {
                            Log.w(TAG, "Consent UI denied by user");
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

        Log.d(TAG, "onCreate()");

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
        credential = DriveConsentFlowHelper.createCredential(this, true);

        loadingOverlay = findViewById(R.id.loadingOverlay);
    }

    private void initSession() {
        primaryEmail = sessionHelper.getPrimaryEmail();
        profileName = sessionHelper.getProfileName();
        if (primaryEmail == null) {
            Log.e(TAG, "Primary email missing, forcing logout");
            forceLogout("Session expired. Please login again.");
        } else {
            Log.d(TAG, "Primary account: " + primaryEmail);
        }
    }

    private void initUI() {
        profileInfoView = findViewById(R.id.profileInfo);
        storageBar = findViewById(R.id.storageBar);

        bindProfileHeader();

        ((TextView) findViewById(R.id.tvUserEmail))
                .setText(profileName != null ? profileName : primaryEmail);

        findViewById(R.id.btnAddTrustedAccount)
                .setOnClickListener(v -> {
                    Log.d(TAG, "Add trusted account clicked");
                    accountPickerLauncher.launch(
                            credential.newChooseAccountIntent()
                    );
                });

        findViewById(R.id.btnLogout)
                .setOnClickListener(v -> logout());
    }

    private void bindProfileHeader() {

        // ---- Resolve display name ----
        String displayName;
        String displayEmail;

        if (profileName != null && !profileName.isEmpty()) {
            displayName = profileName;
            displayEmail = primaryEmail;
        } else {
            displayName = primaryEmail;
            displayEmail = ""; // ProfileInfoView will still render cleanly
        }

        // ---- Load cached profile photo ----
        Bitmap profileBitmap =
                GoogleUserProfileFetcher.loadSavedProfilePhoto(this);

        // ---- Bind to custom view ----
        profileInfoView.setProfile(
                profileBitmap,
                displayName,
                displayEmail
        );
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
                                Log.d(TAG, "Trusted account picker cancelled");
                            }
                        }
                );
    }

    /* ---------------- Primary Account Consent ---------------- */

    private void checkPrimaryAccountConsentIfNeeded(boolean fromLogin) {
        if (fromLogin) {
            Log.d(TAG, "Dashboard opened from LoginActivity, skipping primary consent check");
            return;
        }

        Log.d(TAG, "Checking Drive consent for primary account");

        credential.setSelectedAccount(
                new Account(primaryEmail, "com.google")
        );

        runConsentCheck(true);
    }

    /* ---------------- Trusted Account Flow ---------------- */

    private void logTrustedAccounts() {

        Log.d(TAG, "Fetching trusted accounts list");

        new Thread(() -> {
            try {
                TrustedAccountsDriveHelper helper =
                        new TrustedAccountsDriveHelper(
                                DashboardActivity.this,
                                primaryEmail
                        );

                var trustedAccounts = helper.getTrustedAccounts();

                Log.d(TAG, "Trusted accounts count = " + trustedAccounts.size());

                for (TrustedAccount account : trustedAccounts) {
                    Log.d(TAG,
                            "Trusted Account ↓\n" +
                                    "  Email        : " + account.email + "\n" +
                                    "  Total Quota  : " + Base.formatBytes(account.totalQuota) + "\n" +
                                    "  Used Quota   : " + Base.formatBytes(account.usedQuota) + "\n" +
                                    "  Free Quota   : " + Base.formatBytes(account.freeQuota)
                    );
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to fetch trusted accounts", e);
            }
        }).start();
    }

    private void handleTrustedAccountPicked(Account account) {
        Log.d(TAG, "Trusted account selected: " + account.name);

        if (account.name.equalsIgnoreCase(primaryEmail)) {
            Toast.makeText(
                    this,
                    "Primary account cannot be added again",
                    Toast.LENGTH_SHORT
            ).show();
            Log.w(TAG, "Primary account selected as trusted — blocked");
            return;
        }

        pendingTrustedAccount = account;
        credential.setSelectedAccount(account);

        Log.d(TAG, "Starting consent check for trusted account");
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

                        Log.d(TAG,
                                (isPrimary ? "Primary" : "Trusted")
                                        + " Drive consent granted"
                        );

                        if (!isPrimary && pendingTrustedAccount != null) {

                            Account trusted = pendingTrustedAccount;
                            pendingTrustedAccount = null;

                            Log.d(TAG, "Starting trusted account registration: " + trusted.name);
                            runOnUiThread(() -> showLoading());

                            new Thread(() -> {
                                try {
                                    TrustedAccountsDriveHelper helper =
                                            new TrustedAccountsDriveHelper(
                                                    DashboardActivity.this,
                                                    primaryEmail
                                            );

                                    helper.addTrustedAccount(trusted.name);

                                    Log.d(TAG, "Trusted account successfully added");

                                    runOnUiThread(() -> {
                                        hideLoading();
                                        Toast.makeText(
                                                DashboardActivity.this,
                                                "Trusted account added ✔",
                                                Toast.LENGTH_SHORT
                                        ).show();
                                        logTrustedAccounts();
                                    });

                                } catch (Exception e) {
                                    Log.e(TAG, "Failed to add trusted account", e);

                                    runOnUiThread(() -> {
                                        hideLoading();
                                        Toast.makeText(
                                                DashboardActivity.this,
                                                "Failed to add trusted account",
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    });
                                }
                            }).start();
                        }
                    }

                    @Override
                    public void onConsentRequired(Intent intent) {
                        if (isFinishing()) return;

                        Log.d(TAG,
                                (isPrimary ? "Primary" : "Trusted")
                                        + " consent UI required"
                        );

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

    /* ---------------- Loading Helpers ---------------- */

    private void showLoading() {
        loadingOverlay.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        loadingOverlay.setVisibility(View.GONE);
    }

    /* ---------------- Logout ---------------- */

    private void forceLogout(String reason) {
        Toast.makeText(this, reason, Toast.LENGTH_LONG).show();
        logout();
    }

    private void logout() {
        Log.d(TAG, "Logging out");
        sessionHelper.logout();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
