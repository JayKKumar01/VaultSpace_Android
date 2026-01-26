package com.github.jaykkumar01.vaultspace.dashboard.helpers;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.core.consent.DriveConsentHelper;
import com.github.jaykkumar01.vaultspace.core.selection.AccountSelectionHelper;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.models.TrustedAccount;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ExpandVaultHelper {

    private static final String TAG = "VaultSpace:ExpandVault";

    /* =======================
     * Result enums
     * ======================= */

    public enum ExpandResult {
        ADDED("Account added successfully"),
        ALREADY_EXISTS("Account already had access");

        private final String message;

        ExpandResult(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }


    public enum ExpandError {
        SELECTION_CANCELLED("Account selection cancelled"),
        NO_ACCOUNT_SELECTED("No account selected"),
        PRIMARY_ACCOUNT_SELECTED("Primary account cannot be added"),
        CONSENT_DENIED("Drive permission is required"),
        CONSENT_FAILED("Failed to verify Drive permission"),
        DRIVE_OPERATION_FAILED("Failed to grant Drive access");

        private final String message;

        ExpandError(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    /* =======================
     * Listener
     * ======================= */

    public interface ExpandAccountListener {
        void onSuccess(@NonNull TrustedAccount account, @NonNull ExpandResult result);

        void onError(@NonNull ExpandError error);
    }

    /* =======================
     * Core
     * ======================= */

    private final String primaryEmail;
    private final TrustedAccountsDriveHelper driveHelper;
    private final AccountSelectionHelper accountSelection;
    private final DriveConsentHelper consentHelper;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ExpandAccountListener listener;

    /* =======================
     * Constructor
     * ======================= */

    public ExpandVaultHelper(@NonNull AppCompatActivity activity) {
        UserSession session = new UserSession(activity);
        this.primaryEmail = session.getPrimaryAccountEmail();
        if (primaryEmail == null) throw new IllegalStateException("Primary account missing");
        this.driveHelper = new TrustedAccountsDriveHelper(activity.getApplicationContext());
        this.accountSelection = new AccountSelectionHelper(activity);
        this.consentHelper = new DriveConsentHelper(activity);
    }

    /* =======================
     * Public API
     * ======================= */

    public void launch(@NonNull ExpandAccountListener listener) {
        if (this.listener != null) return;
        this.listener = listener;

        accountSelection.launch(new AccountSelectionHelper.Callback() {
            @Override
            public void onAccountSelected(String email) {
                handleAccountPicked(email);
            }

            @Override
            public void onCancelled() {
                completeError(ExpandError.SELECTION_CANCELLED);
            }
        });
    }

    /* =======================
     * Flow steps
     * ======================= */

    private void handleAccountPicked(String email) {
        if (email == null) {
            completeError(ExpandError.NO_ACCOUNT_SELECTED);
            return;
        }
        if (email.equalsIgnoreCase(primaryEmail)) {
            completeError(ExpandError.PRIMARY_ACCOUNT_SELECTED);
            return;
        }
        ensureConsent(email);
    }

    private void ensureConsent(String email) {
        consentHelper.launch(email, new DriveConsentHelper.Callback() {
            @Override
            public void onConsentGranted(String e) {
                grantDriveAccess(e);
            }

            @Override
            public void onConsentDenied(String e) {
                completeError(ExpandError.CONSENT_DENIED);
            }

            @Override
            public void onFailure(String e, Exception ex) {
                completeError(ExpandError.CONSENT_FAILED);
            }
        });
    }

    private void grantDriveAccess(String email) {
        driveHelper.addTrustedAccount(executor, email, new TrustedAccountsDriveHelper.AddCallback() {

            @Override
            public void onAdded(TrustedAccount account) {
                completeSuccess(account, ExpandResult.ADDED);
            }

            @Override
            public void onAlreadyExists(TrustedAccount account) {
                completeSuccess(account, ExpandResult.ALREADY_EXISTS);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Grant access failed", e);
                completeError(ExpandError.DRIVE_OPERATION_FAILED);
            }
        });
    }

    /* =======================
     * Terminal helpers
     * ======================= */

    private void completeSuccess(TrustedAccount account, ExpandResult result) {
        if (listener == null) return;
        runOnUi(() -> {
            listener.onSuccess(account, result);
            listener = null;
        });
    }

    private void completeError(ExpandError error) {
        if (listener == null) return;
        runOnUi(() -> {
            listener.onError(error);
            listener = null;
        });
    }

    private void runOnUi(Runnable r) {
        mainHandler.post(r);
    }

    /* =======================
     * Cleanup
     * ======================= */

    public void release() {
        executor.shutdown();
        listener = null;
    }
}
