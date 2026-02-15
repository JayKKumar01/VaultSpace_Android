package com.github.jaykkumar01.vaultspace.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.core.consent.PrimaryAccountConsentHelper;
import com.github.jaykkumar01.vaultspace.core.selection.AccountSelectionHelper;
import com.github.jaykkumar01.vaultspace.core.session.PrimaryUserCoordinator;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.views.popups.confirm.ConfirmSpec;
import com.github.jaykkumar01.vaultspace.views.popups.confirm.ConfirmView;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalEnums;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalHost;
import com.github.jaykkumar01.vaultspace.views.popups.loading.LoadingSpec;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpace:Login";

    private AccountSelectionHelper accountSelectionHelper;
    private PrimaryAccountConsentHelper primaryConsentHelper;

    private ModalHost modalHost;
    private LoadingSpec loadingSpec;
    private ConfirmSpec exitAppSpec;

    private String pendingEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        new UserSession(this).clearSession();

        modalHost = ModalHost.attach(this);
        loadingSpec = new LoadingSpec();
        exitAppSpec = new ConfirmSpec(
                "Exit VaultSpace?",
                "Are you sure you want to exit the app?",
                true,
                ConfirmView.RISK_WARNING,
                this::finish,
                null
        );

        initBackHandling();
        initHelpers();
        initUI();
    }

    private void initHelpers() {
        primaryConsentHelper = new PrimaryAccountConsentHelper(this);
        accountSelectionHelper = new AccountSelectionHelper(this);
    }

    private void initUI() {
        findViewById(R.id.btnSelectPrimaryAccount)
                .setOnClickListener(v -> onSelectAccountClicked());
    }

    private void onSelectAccountClicked() {
        showLoading();
        accountSelectionHelper.launch(new AccountSelectionHelper.Callback() {

            @Override
            public void onAccountSelected(String email) {
                handleAccountSelected(email);
            }

            @Override
            public void onCancelled() {
                hideLoading(); // defensive
            }
        });
    }

    private void handleAccountSelected(String email) {
        pendingEmail = email;
        showLoading();

        primaryConsentHelper.startLoginConsentFlow(
                email,
                new PrimaryAccountConsentHelper.LoginCallback() {

                    @Override
                    public void onAllConsentsGranted() {
                        finalizeLogin();
                    }

                    @Override
                    public void onConsentDenied() {
                        hideLoading();
                        toast("Required permissions not granted");
                    }

                    @Override
                    public void onFailure(Exception e) {
                        hideLoading();
                        toast("Failed to verify permissions");
                    }
                }
        );
    }

    private void finalizeLogin() {
        if (pendingEmail == null) {
            Log.w(TAG, "finalizeLogin() called with null email");
            hideLoading();
            toast("Something went wrong. Please try again.");
            return;
        }

        PrimaryUserCoordinator.prepare(
                getApplicationContext(),
                pendingEmail,
                new PrimaryUserCoordinator.Callback() {

                    @Override
                    public void onSuccess() {
                        hideLoading();
                        navigateToDashboard();
                    }

                    @Override
                    public void onError() {
                        hideLoading();
                        toast("Failed to set up your account. Please try again.");
                    }
                }
        );
    }

    private void navigateToDashboard() {
        startActivity(
                new Intent(this, DashboardActivity.class)
                        .putExtra("FROM_LOGIN", true)
        );
        finish();
    }

    /* ---------- Modal helpers (new system) ---------- */

    private void showLoading() {

        modalHost.request(loadingSpec);
    }

    private void hideLoading() {
        modalHost.dismiss(loadingSpec, ModalEnums.DismissResult.SYSTEM);
    }

    private void initBackHandling() {
        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (modalHost.onBackPressed()) {
                            return;
                        }
                        modalHost.request(exitAppSpec);
                    }
                }
        );
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}