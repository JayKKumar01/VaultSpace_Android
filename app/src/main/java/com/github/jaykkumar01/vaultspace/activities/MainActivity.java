package com.github.jaykkumar01.vaultspace.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.core.consent.PrimaryAccountConsentHelper;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.views.popups.confirm.ConfirmSpec;
import com.github.jaykkumar01.vaultspace.views.popups.confirm.ConfirmView;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalEnums.DismissResult;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalHost;
import com.github.jaykkumar01.vaultspace.views.popups.loading.LoadingSpec;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "VaultSpace:MainBoot";

    private enum BootState {
        IDLE,
        VALIDATING_SESSION,
        VALIDATING_CONSENT,
        WAITING_RETRY,
        FINISHED
    }

    private BootState state = BootState.IDLE;

    private UserSession userSession;
    private PrimaryAccountConsentHelper consentHelper;
    private String primaryEmail;

    /* ---------- Modals ---------- */

    private ModalHost modalHost;
    private final LoadingSpec loadingSpec = new LoadingSpec();
    private ConfirmSpec retryConsentSpec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        applyWindowInsets();

        modalHost = ModalHost.attach(this);

        userSession = new UserSession(this);
        consentHelper = new PrimaryAccountConsentHelper(this);

        initRetrySpec();

        moveToState(BootState.VALIDATING_SESSION);
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    /* ==========================================================
     * Modal Setup
     * ========================================================== */

    private void initRetrySpec() {
        retryConsentSpec = new ConfirmSpec(
                "Connection issue",
                "Unable to verify permissions right now.",
                true,
                ConfirmView.RISK_NEUTRAL,
                null,
                null
        );
        retryConsentSpec.setPositiveText("Retry");
        retryConsentSpec.setNegativeText("Exit");
        retryConsentSpec.setCancelable(false);
    }

    /* ==========================================================
     * State Machine
     * ========================================================== */

    private void moveToState(@NonNull BootState newState) {
        if (state == BootState.FINISHED) return;
        if (state == newState) return;

        state = newState;

        switch (newState) {

            case VALIDATING_SESSION:
                handleSessionValidation();
                break;

            case VALIDATING_CONSENT:
                handleConsentValidation();
                break;

            case WAITING_RETRY:
                showRetryModal();
                break;

            case FINISHED:
                finish();
                break;

            case IDLE:
            default:
                break;
        }
    }

    /* ==========================================================
     * Session Validation
     * ========================================================== */

    private void handleSessionValidation() {
        if (!userSession.isLoggedIn()) {
            launchLogin();
            return;
        }

        primaryEmail = userSession.getPrimaryAccountEmail();

        if (primaryEmail == null) {
            userSession.clearSession();
            launchLogin();
            return;
        }

        moveToState(BootState.VALIDATING_CONSENT);
    }

    /* ==========================================================
     * Consent Validation
     * ========================================================== */

    private void handleConsentValidation() {

        modalHost.request(loadingSpec);

        consentHelper.checkConsentsSilently(primaryEmail, result -> {

            modalHost.dismiss(loadingSpec, DismissResult.SYSTEM);

            switch (result) {

                case GRANTED:
                    launchDashboard();
                    break;

                case TEMPORARY_UNAVAILABLE:
                    moveToState(BootState.WAITING_RETRY);
                    break;

                default:
                    userSession.clearSession();
                    launchLogin();
                    break;
            }
        });
    }

    /* ==========================================================
     * Retry Handling (ModalHost)
     * ========================================================== */

    private void showRetryModal() {

        retryConsentSpec.setPositiveAction(() ->
                moveToState(BootState.VALIDATING_SESSION)
        );

        retryConsentSpec.setNegativeAction(() ->
                moveToState(BootState.FINISHED)
        );

        modalHost.request(retryConsentSpec);
    }

    /* ==========================================================
     * Navigation
     * ========================================================== */


    private void launchDashboard() {
        if (state == BootState.FINISHED) return;

        state = BootState.FINISHED;

        Log.d(TAG, "Boot SUCCESS → Consent GRANTED. Dashboard would launch now.");
    }


    private void launchLogin() {
        if (state == BootState.FINISHED) return;
        state = BootState.FINISHED;
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        modalHost.dismiss(loadingSpec, DismissResult.SYSTEM);
        if (retryConsentSpec != null) {
            modalHost.dismiss(retryConsentSpec, DismissResult.SYSTEM);
        }
    }
}
