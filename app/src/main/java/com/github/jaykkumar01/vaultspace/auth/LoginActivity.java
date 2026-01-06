package com.github.jaykkumar01.vaultspace.auth;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.core.auth.DriveConsentFlowHelper;
import com.github.jaykkumar01.vaultspace.core.auth.GoogleAccountPickerHelper;
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

        Log.d(TAG, "onCreate");

        userSession = new UserSession(this);
        consentHelper = new DriveConsentFlowHelper();
        credential = DriveConsentFlowHelper.createCredential(this);

        accountPickerHelper =
                new GoogleAccountPickerHelper(
                        accountPickerLauncher,
                        new GoogleAccountPickerHelper.Callback() {
                            @Override
                            public void onAccountPicked(Account account) {
                                Log.d(TAG, "Primary account selected: " + account.name);
                                pendingAccount = account;
                                credential.setSelectedAccount(account);
                                checkConsent();
                            }

                            @Override
                            public void onCancelled() {
                                Log.d(TAG, "Account picker cancelled by user");
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

    private void onAccountPickerResult(
            androidx.activity.result.ActivityResult result
    ) {
        accountPickerHelper.handleResult(
                result.getResultCode(),
                result.getData()
        );
    }

    /* ---------------- Consent Flow ---------------- */

    private void checkConsent() {
        Log.d(TAG, "Starting Drive consent flow");

        consentHelper.checkConsent(
                this,
                credential,
                new DriveConsentFlowHelper.Callback() {
                    @Override
                    public void onConsentGranted() {
                        Log.d(TAG, "Drive consent already granted");
                        finalizeLogin();
                    }

                    @Override
                    public void onConsentRequired(Intent intent) {
                        Log.d(TAG, "Launching Drive consent UI");
                        consentLauncher.launch(intent);
                    }

                    @Override
                    public void onFailure(Exception e) {
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

        Intent i = new Intent(this, DashboardActivity.class);
        i.putExtra("FROM_LOGIN", true);
        startActivity(i);
        finish();
    }

    /* ---------------- UI Helpers ---------------- */

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
