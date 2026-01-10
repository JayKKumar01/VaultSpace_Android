package com.github.jaykkumar01.vaultspace.dashboard.helpers;

import android.app.Activity;
import android.util.Log;

import com.github.jaykkumar01.vaultspace.views.popups.ActivityLoadingOverlay;
import com.github.jaykkumar01.vaultspace.views.popups.ConfirmActionView;

public final class DashboardConfirmHelper {

    private static final String TAG = "VaultSpace:DashboardConfirm";

    public interface ExitHandler {
        void exitToLogin(String reason);
    }

    private final Activity activity;
    private final ActivityLoadingOverlay loading;
    private final ConfirmActionView confirmView;
    private final ExitHandler exitHandler;

    public DashboardConfirmHelper(
            Activity activity,
            ActivityLoadingOverlay loading,
            ExitHandler exitHandler
    ) {
        this.activity = activity;
        this.loading = loading;
        this.exitHandler = exitHandler;
        this.confirmView = ConfirmActionView.attach(activity);
    }

    /* ----------------------------------------------------------
     * Global helpers
     * ---------------------------------------------------------- */

    public boolean isVisible() {
        return confirmView.isVisible();
    }

    public void dismissIfVisible() {
        if (confirmView.isVisible()) {
            confirmView.hide();
            Log.d(TAG, "Confirm dismissed");
        }
    }

    private void prepareForConfirm() {
        // Hard rule: confirm and loading never overlap
        loading.hide();
    }

    /* ----------------------------------------------------------
     * Confirm types
     * ---------------------------------------------------------- */

    public void showCancelSetupConfirm(Runnable onCancelAbort) {
        if (confirmView.isVisible()) return;
        prepareForConfirm();

        confirmView.show(
                "Cancel setup?",
                "This will stop verification and return you to login.",
                "Return to login",
                ConfirmActionView.RISK_WARNING,
                "Dashboard:CancelSetup",
                new ConfirmActionView.Callback() {
                    @Override
                    public void onConfirm() {
                        exitHandler.exitToLogin("Setup cancelled");
                    }

                    @Override
                    public void onCancel() {
                        if (onCancelAbort != null) onCancelAbort.run();
                    }
                }
        );
    }

    public void showExitConfirm() {
        if (confirmView.isVisible()) return;
        prepareForConfirm();

        confirmView.show(
                "Exit VaultSpace?",
                "Are you sure you want to exit the app?",
                "Exit",
                ConfirmActionView.RISK_WARNING,
                "Dashboard:Exit",
                new ConfirmActionView.Callback() {
                    @Override
                    public void onConfirm() {
                        activity.finish();
                    }

                    @Override
                    public void onCancel() {
                        Log.d(TAG, "Exit cancelled");
                    }
                }
        );
    }

    public void showLogoutConfirm() {
        if (confirmView.isVisible()) return;
        prepareForConfirm();

        confirmView.show(
                "Log out?",
                "You’ll need to sign in again to access your vault.",
                "Log out",
                ConfirmActionView.RISK_DESTRUCTIVE,
                "Dashboard:Logout",
                new ConfirmActionView.Callback() {
                    @Override
                    public void onConfirm() {
                        exitHandler.exitToLogin("You have been logged out");
                    }

                    @Override
                    public void onCancel() {
                        Log.d(TAG, "Logout cancelled");
                    }
                }
        );
    }
}
