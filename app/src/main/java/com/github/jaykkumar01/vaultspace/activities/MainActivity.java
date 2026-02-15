package com.github.jaykkumar01.vaultspace.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.core.consent.PrimaryAccountConsentHelper;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.views.anim.SplashBorderView;
import com.github.jaykkumar01.vaultspace.views.popups.confirm.ConfirmSpec;
import com.github.jaykkumar01.vaultspace.views.popups.confirm.ConfirmView;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalEnums.DismissResult;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalHost;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpace:MainBoot";

    /* ==========================================================
       Boot State
       ========================================================== */

    private enum BootState {
        IDLE,
        VALIDATING_SESSION,
        VALIDATING_CONSENT,
        WAITING_RETRY,
        FINISHED
    }

    private BootState state = BootState.IDLE;

    /* ==========================================================
       Core
       ========================================================== */

    private UserSession userSession;
    private PrimaryAccountConsentHelper consentHelper;
    private String primaryEmail;

    /* ==========================================================
       Modal System
       ========================================================== */

    private ModalHost modalHost;
    private ConfirmSpec retryConsentSpec;
    private ConfirmSpec cancelBootSpec;

    /* ==========================================================
       Lifecycle
       ========================================================== */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        initCore();
        initSpecs();
        initBackHandler();

        moveToState(BootState.VALIDATING_SESSION);
    }

    @Override
    protected void onDestroy() {
        modalHost.dismiss(retryConsentSpec, DismissResult.SYSTEM);
        modalHost.dismiss(cancelBootSpec, DismissResult.SYSTEM);
        super.onDestroy();
    }


    /* ==========================================================
       Initialization
       ========================================================== */

    private void initCore() {
        modalHost = ModalHost.attach(this);
        userSession = new UserSession(this);
        consentHelper = new PrimaryAccountConsentHelper(this);
    }

    private void initSpecs() {

        retryConsentSpec = new ConfirmSpec(
                "Connection issue",
                "Unable to verify permissions right now.",
                true,
                ConfirmView.RISK_NEUTRAL,
                () -> moveToState(BootState.VALIDATING_SESSION),
                () -> moveToState(BootState.FINISHED)
        );
        retryConsentSpec.setPositiveText("Retry");
        retryConsentSpec.setNegativeText("Exit");
        retryConsentSpec.setCancelable(false);

        cancelBootSpec = new ConfirmSpec(
                "Cancel startup?",
                "This will stop validation and return you to login.",
                true,
                ConfirmView.RISK_DESTRUCTIVE,
                () -> moveToState(BootState.FINISHED),
                null
        );
        cancelBootSpec.setNegativeText("Continue");
    }

    private void initBackHandler() {
        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {

                        if (modalHost.onBackPressed())
                            return;

                        if (state == BootState.WAITING_RETRY)
                            return;

                        modalHost.request(cancelBootSpec);
                    }
                }
        );
    }

    /* ==========================================================
       State Machine
       ========================================================== */

    private void moveToState(@NonNull BootState newState) {

        if (state == BootState.FINISHED) return;
        if (state == newState) return;

        Log.d(TAG, "State → " + newState);
        state = newState;

        switch (newState) {

            case VALIDATING_SESSION:
                validateSession();
                break;

            case VALIDATING_CONSENT:
                validateConsent();
                break;

            case WAITING_RETRY:
                modalHost.request(retryConsentSpec);
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
       Validation Logic
       ========================================================== */

    private void validateSession() {

        if (userSession.isLoggedOut()) {
            navigateToLogin();
            return;
        }

        primaryEmail = userSession.getPrimaryAccountEmail();

        if (primaryEmail == null) {
            userSession.clearSession();
            navigateToLogin();
            return;
        }

        moveToState(BootState.VALIDATING_CONSENT);
    }

    private void validateConsent() {

        consentHelper.checkConsentsSilently(primaryEmail, result -> {

            if (state == BootState.FINISHED || isFinishing() || isDestroyed())
                return;

            switch (result) {

                case GRANTED:
                    navigateToDashboard();
                    break;

                case TEMPORARY_UNAVAILABLE:
                    moveToState(BootState.WAITING_RETRY);
                    break;

                default:
                    userSession.clearSession();
                    navigateToLogin();
                    break;
            }
        });
    }

    /* ==========================================================
       Navigation
       ========================================================== */

    private void navigateToDashboard() {

        SplashBorderView border = findViewById(R.id.splashBorderOverlay);

        border.clearAnimationNow(() -> {

            if (isFinishing() || isDestroyed())
                return;

            startActivity(new Intent(this, DashboardActivity.class));
            moveToState(BootState.FINISHED);
        });
    }

    private void navigateToLogin() {

        SplashBorderView border = findViewById(R.id.splashBorderOverlay);

        border.clearAnimationNow(() -> {

            if (isFinishing() || isDestroyed())
                return;

            startActivity(new Intent(this, LoginActivity.class));
            moveToState(BootState.FINISHED);
        });
    }
}
