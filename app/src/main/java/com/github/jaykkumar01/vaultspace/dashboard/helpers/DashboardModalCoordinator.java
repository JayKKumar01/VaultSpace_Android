package com.github.jaykkumar01.vaultspace.dashboard.helpers;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.activities.DashboardActivity;
import com.github.jaykkumar01.vaultspace.views.popups.confirm.ConfirmSpec;
import com.github.jaykkumar01.vaultspace.views.popups.confirm.ConfirmView;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalEnums.DismissResult;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalHost;
import com.github.jaykkumar01.vaultspace.views.popups.loading.LoadingSpec;

public final class DashboardModalCoordinator {

    private final ModalHost modalHost;
    private final Runnable exitToLogin;

    /* ---------- Stable specs ---------- */

    private final LoadingSpec loadingSpec = new LoadingSpec();

    private final ConfirmSpec cancelSetupSpec;
    private final ConfirmSpec exitAppSpec;
    private final ConfirmSpec logoutSpec;
    private final ConfirmSpec retryConsentSpec;

    public DashboardModalCoordinator(
            @NonNull ModalHost modalHost,
            @NonNull Runnable exitToLogin
    ) {
        this.modalHost = modalHost;
        this.exitToLogin = exitToLogin;

        cancelSetupSpec = new ConfirmSpec(
                "Cancel setup?",
                "This will stop verification and return you to login.",
                true,
                ConfirmView.RISK_DESTRUCTIVE,
                exitToLogin,
                null
        );

        exitAppSpec = new ConfirmSpec(
                "Exit VaultSpace?",
                "Are you sure you want to exit the app?",
                true,
                ConfirmView.RISK_WARNING,
                null,
                null
        );

        logoutSpec = new ConfirmSpec(
                "Log out?",
                "You’ll need to sign in again to access your vault.",
                true,
                ConfirmView.RISK_CRITICAL,
                exitToLogin,
                null
        );

        retryConsentSpec = new ConfirmSpec(
                "Connection issue",
                "Unable to verify permissions right now.",
                true,
                ConfirmView.RISK_CRITICAL,
                null,
                null
        );
        retryConsentSpec.setPositiveText("Retry");
        retryConsentSpec.setNegativeText("Exit");
    }

    /* ---------- Loading ---------- */

    public void showLoading() {
        modalHost.request(loadingSpec);
    }

    public void clearLoading() {
        modalHost.dismiss(loadingSpec, DismissResult.SYSTEM);
    }

    /* ---------- Back press ---------- */

    public void handleBackPress(
            @NonNull DashboardActivity.AuthState state,
            @NonNull Runnable exitApp,
            @NonNull Runnable retryChecking
    ) {
        switch (state) {

            case INIT:
                modalHost.request(cancelSetupSpec);
                break;

            case CHECKING:
                confirmRetryConsent(
                        retryChecking,
                        exitApp
                );
                break;

            case GRANTED:
                exitAppSpec.setPositiveAction(exitApp);
                modalHost.request(exitAppSpec);
                break;
        }
    }


    /* ---------- Explicit actions ---------- */

    public void confirmLogout() {
        modalHost.request(logoutSpec);
    }

    public void confirmRetryConsent(
            @NonNull Runnable onRetry,
            @NonNull Runnable onExit
    ) {
        retryConsentSpec.setPositiveAction(onRetry);
        retryConsentSpec.setNegativeAction(onExit);
        modalHost.request(retryConsentSpec);
    }

    /* ---------- Reset ---------- */

    public void reset() {
        modalHost.dismiss(loadingSpec, DismissResult.SYSTEM);
        modalHost.dismiss(cancelSetupSpec, DismissResult.SYSTEM);
        modalHost.dismiss(retryConsentSpec, DismissResult.SYSTEM);
    }
}
