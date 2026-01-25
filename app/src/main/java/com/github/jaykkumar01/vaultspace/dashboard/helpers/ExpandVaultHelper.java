package com.github.jaykkumar01.vaultspace.dashboard.helpers;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.core.consent.DriveConsentHelper;
import com.github.jaykkumar01.vaultspace.core.drive.TrustedAccountsRepository;
import com.github.jaykkumar01.vaultspace.core.selection.AccountSelectionHelper;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.models.TrustedAccount;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ExpandVaultHelper {

    private static final String TAG = "VaultSpace:ExpandVault";

    /* ==========================================================
     * Listeners
     * ========================================================== */

    public interface ExpandAccountListener {
        void onSuccess();
        void onError(@NonNull String message);
    }



    /* ==========================================================
     * Core
     * ========================================================== */

    private final String primaryEmail;

    private final TrustedAccountsRepository repo;
    private final TrustedAccountsDriveHelper driveHelper;
    private final AccountSelectionHelper accountSelection;
    private final DriveConsentHelper consentHelper;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ExpandAccountListener actionListener;


    /* ==========================================================
     * Constructor
     * ========================================================== */

    public ExpandVaultHelper(@NonNull AppCompatActivity activity, TrustedAccountsRepository repo) {

        UserSession session = new UserSession(activity);
        this.primaryEmail = session.getPrimaryAccountEmail();

        if (primaryEmail == null) {
            throw new IllegalStateException("Primary email is null");
        }
        this.repo = repo;
        this.driveHelper = new TrustedAccountsDriveHelper(activity.getApplicationContext());
        this.accountSelection = new AccountSelectionHelper(activity);
        this.consentHelper = new DriveConsentHelper(activity);
    }

    /* ==========================================================
     * Expand vault flow
     * ========================================================== */

    public void launchExpandVault(@NonNull ExpandAccountListener listener) {
        this.actionListener = listener;

        accountSelection.launch(new AccountSelectionHelper.Callback() {
            @Override
            public void onAccountSelected(String email) {
                handleAccountPicked(email);
            }

            @Override
            public void onCancelled() {
                failAction("Account selection cancelled");
            }
        });
    }

    private void handleAccountPicked(String email) {
        if (email == null) {
            failAction("No account selected");
            return;
        }

        if (email.equalsIgnoreCase(primaryEmail)) {
            failAction("Primary account cannot be added");
            return;
        }

        requestConsent(email);
    }

    private void requestConsent(String email) {
        consentHelper.launch(
                email,
                new DriveConsentHelper.Callback() {
                    @Override
                    public void onConsentGranted(String email) {
                        addTrustedAccount(email);
                    }

                    @Override
                    public void onConsentDenied(String email) {
                        failAction("Drive permission is required");
                    }

                    @Override
                    public void onFailure(String email, Exception e) {
                        failAction("Failed to verify Drive permissions");
                    }
                }
        );
    }

    private void addTrustedAccount(String email) {
        driveHelper.addTrustedAccount(
                executor,
                email,
                new TrustedAccountsDriveHelper.AddCallback() {
                    @Override
                    public void onAdded(TrustedAccount account) {
                        repo.addAccount(account);
                    }

                    @Override
                    public void onAlreadyExists(TrustedAccount account) {
                        repo.addAccount(account);
                        failAction("Account already has access");
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Add trusted account failed", e);
                        failAction("Failed to grant access");
                    }
                }
        );
    }

    /* ==========================================================
     * Action helpers (guaranteed single callback)
     * ========================================================== */

    private void succeedAction() {
        if (actionListener == null) return;

        runOnUi(() -> {
            actionListener.onSuccess();
            actionListener = null;
        });
    }

    private void failAction(String message) {
        if (actionListener == null) return;

        runOnUi(() -> {
            actionListener.onError(message);
            actionListener = null;
        });
    }

    /* ==========================================================
     * Cleanup
     * ========================================================== */

    private void runOnUi(Runnable r) {
        mainHandler.post(r);
    }

    public void release() {
        executor.shutdown();
        actionListener = null;
    }

}
