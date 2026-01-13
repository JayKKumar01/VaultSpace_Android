package com.github.jaykkumar01.vaultspace.dashboard.helpers;

import android.util.Log;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.activities.DashboardActivity;
import com.github.jaykkumar01.vaultspace.views.popups.confirm.ConfirmSpec;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalEnums.*;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalHost;
import com.github.jaykkumar01.vaultspace.views.popups.loading.LoadingSpec;

public final class DashboardModalCoordinator {

    private static final String TAG = "VaultSpace:DashboardModal";

    private final ModalHost modalHost;
    private final Runnable exitToLogin;

    /* =======================
       Specs (stable instances)
       ======================= */

    private final LoadingSpec loadingSpec;
    private final ConfirmSpec cancelSetupSpec;
    private final ConfirmSpec exitAppSpec;
    private final ConfirmSpec logoutSpec;

    /* =======================
       Debug counters (loading)
       ======================= */

    private int loadingReq = 0;
    private int loadingClear = 0;

    public DashboardModalCoordinator(
            @NonNull ModalHost modalHost,
            @NonNull Runnable exitToLogin
    ) {
        this.modalHost = modalHost;
        this.exitToLogin = exitToLogin;

        loadingSpec = new LoadingSpec();

        cancelSetupSpec = new ConfirmSpec(
                "Cancel setup?",
                "This will stop verification and return you to login.",
                true,
                Priority.HIGH,
                exitToLogin,
                null
        );

        exitAppSpec = new ConfirmSpec(
                "Exit VaultSpace?",
                "Are you sure you want to exit the app?",
                true,
                Priority.MEDIUM,
                null,   // assigned at call time
                null
        );

        logoutSpec = new ConfirmSpec(
                "Log out?",
                "You’ll need to sign in again to access your vault.",
                true,
                Priority.MEDIUM,
                exitToLogin,
                null
        );
    }

    /* ==========================================================
     * Loading (STATE)
     * ========================================================== */

    public void showLoading() {
        loadingReq++;
        Log.d(TAG, "showLoading() #" + loadingReq);
        modalHost.request(loadingSpec);
    }

    public void clearLoading() {
        loadingClear++;
        Log.d(TAG, "clearLoading() #" + loadingClear);
        modalHost.dismiss(loadingSpec, DismissResult.SYSTEM);
    }

    /* ==========================================================
     * Back press semantics
     * ========================================================== */

    public void handleBackPress(
            @NonNull DashboardActivity.AuthState state,
            @NonNull Runnable exitApp
    ) {
        switch (state) {
            case INIT:
            case CHECKING:
                modalHost.request(cancelSetupSpec);
                break;

            case GRANTED:
                // bind action just before request (no new spec)
                exitAppSpec.setPositiveAction(exitApp);
                modalHost.request(exitAppSpec);
                break;
        }
    }

    /* ==========================================================
     * Explicit actions
     * ========================================================== */

    public void confirmLogout() {
        modalHost.request(logoutSpec);
    }

    /* ==========================================================
     * Reset
     * ========================================================== */

    public void reset() {
        modalHost.dismiss(loadingSpec, DismissResult.SYSTEM);
        modalHost.dismiss(cancelSetupSpec, DismissResult.SYSTEM);
//        modalHost.dismiss(exitAppSpec, DismissResult.SYSTEM);
//        modalHost.dismiss(logoutSpec, DismissResult.SYSTEM);

        Log.d(TAG,
                "reset() loadingReq=" + loadingReq +
                        " loadingClear=" + loadingClear);
    }
}
