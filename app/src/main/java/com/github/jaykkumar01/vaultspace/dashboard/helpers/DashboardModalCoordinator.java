package com.github.jaykkumar01.vaultspace.dashboard.helpers;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.views.popups.old.confirm.ConfirmRisk;
import com.github.jaykkumar01.vaultspace.views.popups.old.confirm.ConfirmSpec;
import com.github.jaykkumar01.vaultspace.views.popups.old.core.ModalDismissReason;
import com.github.jaykkumar01.vaultspace.views.popups.old.core.ModalHostView;
import com.github.jaykkumar01.vaultspace.views.popups.old.loading.LoadingSpec;

public final class DashboardModalCoordinator {

    private final ModalHostView modalHost;
    private final Runnable exitToLogin;

    // STATE modal (spec-type scoped dismiss)
    private final LoadingSpec loadingSpec = new LoadingSpec();

    public DashboardModalCoordinator(
            @NonNull ModalHostView modalHost,
            @NonNull Runnable exitToLogin
    ) {
        this.modalHost = modalHost;
        this.exitToLogin = exitToLogin;
    }

    /* ==========================================================
     * Loading (STATE)
     * ========================================================== */

    public void showLoading() {
        modalHost.request(loadingSpec);
    }

    public void clearLoading() {
        modalHost.dismiss(loadingSpec, ModalDismissReason.SYSTEM);
    }

    /* ==========================================================
     * Back press semantics
     * ========================================================== */

    public void handleBackPress(
            @NonNull DashboardAuthState state,
            @NonNull Runnable exitApp
    ) {
        switch (state) {
            case INIT:
            case CHECKING:
                showCancelSetupConfirm();
                break;

            case GRANTED:
                showExitConfirm(exitApp);
                break;
        }
    }

    /* ==========================================================
     * Explicit actions
     * ========================================================== */

    public void confirmLogout() {
        modalHost.request(
                new ConfirmSpec(
                        "Log out?",
                        "You’ll need to sign in again to access your vault.",
                        "Log out",
                        true,
                        true,
                        true,
                        ConfirmRisk.RISK_CRITICAL,
                        exitToLogin,
                        null
                )
        );
    }

    /* ==========================================================
     * Reset
     * ========================================================== */

    public void reset() {
        clearLoading();
        // EVENT modals auto-clear via replacement / back press
    }

    /* ==========================================================
     * Internal confirms
     * ========================================================== */

    private void showCancelSetupConfirm() {
        modalHost.request(
                new ConfirmSpec(
                        "Cancel setup?",
                        "This will stop verification and return you to login.",
                        "Return to login",
                        true,
                        true,
                        true,
                        ConfirmRisk.RISK_WARNING,
                        exitToLogin,
                        null
                )
        );
    }

    private void showExitConfirm(@NonNull Runnable exitApp) {
        modalHost.request(
                new ConfirmSpec(
                        "Exit VaultSpace?",
                        "Are you sure you want to exit the app?",
                        "Exit",
                        true,
                        true,
                        true,
                        ConfirmRisk.RISK_WARNING,
                        exitApp,
                        null
                )
        );
    }

    /* ==========================================================
     * Auth state mirror (minimal enum)
     * ========================================================== */

    public enum DashboardAuthState {
        INIT,
        CHECKING,
        GRANTED
    }
}
