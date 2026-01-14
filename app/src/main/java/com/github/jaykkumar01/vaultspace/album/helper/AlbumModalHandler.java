package com.github.jaykkumar01.vaultspace.album.helper;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.views.popups.confirm.ConfirmSpec;
import com.github.jaykkumar01.vaultspace.views.popups.confirm.ConfirmView;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalEnums.DismissResult;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalHost;

/**
 * AlbumModalCoordinator
 *
 * Responsibilities:
 * - Coordinate album-related modal decisions
 * - Offer Retry / Exit paths for recoverable failures
 *
 * Non-responsibilities:
 * - Album loading
 * - Error classification
 * - UI state management
 */
public final class AlbumModalHandler {

    private final ModalHost modalHost;

    /* ---------- Stable Specs ---------- */

    private final ConfirmSpec retryLoadSpec;
    private final ConfirmSpec permissionRevokedSpec;
    private final ConfirmSpec unrecoverableErrorSpec;

    public AlbumModalHandler(
            @NonNull ModalHost modalHost,
            @NonNull Runnable onRetry,
            @NonNull Runnable onExit
    ) {
        this.modalHost = modalHost;

        /* ---- Generic retry (network / transient) ---- */
        retryLoadSpec = new ConfirmSpec(
                "Unable to load album",
                "Please check your connection and try again.",
                true,
                ConfirmView.RISK_NEUTRAL,
                onRetry,
                onExit
        );
        retryLoadSpec.setPositiveText("Retry");
        retryLoadSpec.setNegativeText("Back");
        retryLoadSpec.setCancelable(false);

        /* ---- Permission revoked (future) ---- */
        permissionRevokedSpec = new ConfirmSpec(
                "Access revoked",
                "You no longer have permission to access this album.",
                false,
                ConfirmView.RISK_WARNING,
                onRetry,      // retry may re-check permissions
                onExit
        );

        /* ---- Unrecoverable error (future) ---- */
        unrecoverableErrorSpec = new ConfirmSpec(
                "Album unavailable",
                "This album can no longer be accessed.",
                false,
                ConfirmView.RISK_CRITICAL,
                null,         // no retry
                onExit
        );
    }

    /* ----------------------------------------------------------
     * Public API (Current usage)
     * ---------------------------------------------------------- */

    /** Network / transient failure */
    public void showRetryLoad() {
        modalHost.request(retryLoadSpec);
    }

    /* ----------------------------------------------------------
     * Public API (Future-ready)
     * ---------------------------------------------------------- */

    /** Permission revoked by user / Drive */
    public void showPermissionRevoked() {
        modalHost.request(permissionRevokedSpec);
    }

    /** Album deleted / unrecoverable */
    public void showUnrecoverableError() {
        modalHost.request(unrecoverableErrorSpec);
    }

    /* ----------------------------------------------------------
     * Lifecycle
     * ---------------------------------------------------------- */

    public void dismissAll() {
        modalHost.dismiss(retryLoadSpec, DismissResult.SYSTEM);
        modalHost.dismiss(permissionRevokedSpec, DismissResult.SYSTEM);
        modalHost.dismiss(unrecoverableErrorSpec, DismissResult.SYSTEM);
    }

    public boolean onBackPressed() {
        return modalHost.onBackPressed();
    }
}
