package com.github.jaykkumar01.vaultspace.login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.core.consent.PrimaryAccountConsentHelper;
import com.github.jaykkumar01.vaultspace.core.picker.AccountPickerHelper;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.dashboard.DashboardActivity;
import com.github.jaykkumar01.vaultspace.utils.GoogleUserProfileFetcher;
import com.github.jaykkumar01.vaultspace.views.ActivityLoadingOverlay;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpace:Login";

    private UserSession userSession;

    private AccountPickerHelper accountPickerHelper;
    private PrimaryAccountConsentHelper primaryConsentHelper;

    private ActivityLoadingOverlay loading;
    private String pendingEmail;

    /* ---------------- Lifecycle ---------------- */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userSession = new UserSession(this);
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
            return;
        }

        Log.d(TAG, "Finalizing login for: " + pendingEmail);

        userSession.savePrimaryAccountEmail(pendingEmail);

        GoogleUserProfileFetcher.fetch(
                getApplicationContext(),
                pendingEmail,
                profileName -> {
                    if (profileName != null) {
                        userSession.saveProfileName(profileName);
                    }
                    navigateToDashboard();
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
