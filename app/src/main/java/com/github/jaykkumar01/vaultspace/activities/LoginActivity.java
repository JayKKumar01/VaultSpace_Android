package com.github.jaykkumar01.vaultspace.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.core.consent.PrimaryAccountConsentHelper;
import com.github.jaykkumar01.vaultspace.core.picker.AccountPickerHelper;
import com.github.jaykkumar01.vaultspace.core.session.PrimaryUserCoordinator;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.utils.GoogleUserProfileFetcher;
import com.github.jaykkumar01.vaultspace.views.popups.ActivityLoadingOverlay;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpace:Login";

    private AccountPickerHelper accountPickerHelper;
    private PrimaryAccountConsentHelper primaryConsentHelper;

    private ActivityLoadingOverlay loading;
    private String pendingEmail;

    /* ---------------- Lifecycle ---------------- */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loading = new ActivityLoadingOverlay(this);

        initHelpers();
        initUI();
    }

    /* ---------------- Init ---------------- */

    private void initHelpers() {
        primaryConsentHelper = new PrimaryAccountConsentHelper(this);
        accountPickerHelper = new AccountPickerHelper(this);
    }

    private void initUI() {
        findViewById(R.id.btnSelectPrimaryAccount)
                .setOnClickListener(v -> onSelectAccountClicked());
    }

    /* ---------------- Account Flow ---------------- */

    private void onSelectAccountClicked() {
        accountPickerHelper.launch(new AccountPickerHelper.Callback() {
            @Override
            public void onAccountSelected(String email) {
                handleAccountSelected(email);
            }

            @Override
            public void onCancelled() {
                loading.hide(); // 👈 safety, in case it was shown
            }
        });
    }

    private void handleAccountSelected(String email) {
        pendingEmail = email;
        loading.show();

        primaryConsentHelper.startLoginConsentFlow(
                email,
                new PrimaryAccountConsentHelper.LoginCallback() {

                    @Override
                    public void onAllConsentsGranted() {
                        finalizeLogin();
                    }

                    @Override
                    public void onConsentDenied() {
                        loading.hide();
                        toast("Required permissions not granted");
                    }

                    @Override
                    public void onFailure(Exception e) {
                        loading.hide();
                        toast("Failed to verify permissions");
                    }
                }
        );
    }

    /* ---------------- Finalization ---------------- */

    private void finalizeLogin() {
        if (pendingEmail == null) {
            Log.w(TAG, "finalizeLogin() called with null email");
            loading.hide();
            toast("Something went wrong. Please try again.");
            return;
        }

        Log.d(TAG, "Finalizing login for: " + pendingEmail);

        PrimaryUserCoordinator.prepare(
                getApplicationContext(),
                pendingEmail,
                new PrimaryUserCoordinator.Callback() {

                    @Override
                    public void onSuccess() {
                        loading.hide();
                        navigateToDashboard();
                    }

                    @Override
                    public void onError() {
                        loading.hide();
                        toast("Failed to set up your account. Please try again.");
                        // stay on Login screen
                    }
                }
        );
    }


    private void navigateToDashboard() {
        loading.hide();
        startActivity(
                new Intent(this, DashboardActivity.class)
                        .putExtra("FROM_LOGIN", true)
        );
        finish();
    }

    /* ---------------- UI Helpers ---------------- */

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
