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

    /* ---------- Stable specs ---------- */

    private final LoadingSpec loadingSpec = new LoadingSpec();
    private final ConfirmSpec exitAppSpec;
    private final ConfirmSpec logoutSpec;

    public DashboardModalCoordinator(@NonNull ModalHost modalHost) {
        this.modalHost = modalHost;

        exitAppSpec = new ConfirmSpec(
                "Exit VaultSpace?",
                "Are you sure you want to exit the app?",
                ConfirmView.RISK_WARNING
        );

        logoutSpec = new ConfirmSpec(
                "Log out?",
                "You’ll need to sign in again to access your vault.",
                ConfirmView.RISK_CRITICAL
        );
    }

    /* ---------- Loading ---------- */

    public void showLoading() {
        modalHost.request(loadingSpec);
    }

    public void clearLoading() {
        modalHost.dismiss(loadingSpec, DismissResult.SYSTEM);
    }



    /* ---------- Explicit actions ---------- */

    public void confirmExit(@NonNull Runnable exitAction) {
        exitAppSpec.onPositive(exitAction);
        modalHost.request(exitAppSpec);
    }


    public void confirmLogout(@NonNull Runnable logoutAction) {
        logoutSpec.onPositive(logoutAction);
        modalHost.request(logoutSpec);
    }


    /* ---------- Reset ---------- */

    public void reset() {
        modalHost.dismiss(logoutSpec, DismissResult.SYSTEM);
        modalHost.dismiss(loadingSpec, DismissResult.SYSTEM);
    }
}
