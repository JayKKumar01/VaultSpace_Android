package com.github.jaykkumar01.vaultspace.dashboard.helpers;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.views.popups.BlockingOverlayView;

public final class DashboardBlockingHelper {

    private final BlockingOverlayView overlay;
    private final Runnable exitToLoginAction;

    public DashboardBlockingHelper(BlockingOverlayView overlay, @NonNull Runnable exitToLoginAction
    ) {
        this.overlay = overlay;
        this.exitToLoginAction = exitToLoginAction;
    }

    /* ==========================================================
     * Loading
     * ========================================================== */

    public void showLoading() {
        overlay.requestLoading();
    }

    public void clearLoading() {
        overlay.clearLoading();
    }

    public void dismissConfirm(){
        overlay.dismissConfirm();
    }

    /* ==========================================================
     * Back press (semantic)
     * ========================================================== */

    /** @return true if back press was consumed */
    public boolean handleBackPress() {
        overlay.dismissConfirm();
        return true;
    }

    /* ==========================================================
     * Confirm flows
     * ========================================================== */

    public void confirmCancelSetup() {
        overlay.showConfirm(
                "Cancel setup?",
                "This will stop verification and return you to login.",
                "Return to login",
                BlockingOverlayView.RISK_WARNING,
                "Dashboard:CancelSetup",
                new BlockingOverlayView.ConfirmCallback() {
                    @Override public void onConfirm() {
                        exitToLoginAction.run();
                    }
                    @Override public void onCancel() { }
                }
        );
    }

    public void confirmExit(@NonNull Runnable exitAppAction) {
        overlay.showConfirm(
                "Exit VaultSpace?",
                "Are you sure you want to exit the app?",
                "Exit",
                BlockingOverlayView.RISK_WARNING,
                "Dashboard:Exit",
                new BlockingOverlayView.ConfirmCallback() {
                    @Override public void onConfirm() {
                        exitAppAction.run();
                    }
                    @Override public void onCancel() { }
                }
        );
    }

    public void confirmLogout() {
        overlay.showConfirm(
                "Log out?",
                "You’ll need to sign in again to access your vault.",
                "Log out",
                BlockingOverlayView.RISK_DESTRUCTIVE,
                "Dashboard:Logout",
                new BlockingOverlayView.ConfirmCallback() {
                    @Override public void onConfirm() {
                        exitToLoginAction.run();
                    }
                    @Override public void onCancel() { }
                }
        );
    }

    /* ==========================================================
     * Hard reset (state transition only)
     * ========================================================== */

    public void resetAll() {
        overlay.reset();
    }
}
