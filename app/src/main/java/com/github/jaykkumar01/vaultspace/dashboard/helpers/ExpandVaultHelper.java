package com.github.jaykkumar01.vaultspace.dashboard.helpers;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.core.consent.DriveConsentHelper;
import com.github.jaykkumar01.vaultspace.core.picker.AccountPickerHelper;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.core.session.cache.TrustedAccountsCache;
import com.github.jaykkumar01.vaultspace.models.TrustedAccount;
import com.github.jaykkumar01.vaultspace.models.VaultStorageState;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ExpandVaultHelper {

    private static final String TAG = "VaultSpace:ExpandVault";
    private static final String UNIT_GB = "GB";
    private static final double BYTES_IN_GB = 1024d * 1024d * 1024d;

    /* ==========================================================
     * Listeners
     * ========================================================== */

    public interface StorageStateListener {
        void onVaultStorageState(@NonNull VaultStorageState state);
    }

    public interface ExpandActionListener {
        void onSuccess();
        void onError(@NonNull String message);
    }

    /* ==========================================================
     * Core
     * ========================================================== */

    private final AppCompatActivity activity;
    private final String primaryEmail;

    private final TrustedAccountsDriveHelper driveHelper;
    private final TrustedAccountsCache cache;
    private final AccountPickerHelper accountPicker;
    private final DriveConsentHelper consentHelper;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private StorageStateListener storageListener;
    private ExpandActionListener actionListener;

    /* ==========================================================
     * Constructor
     * ========================================================== */

    public ExpandVaultHelper(@NonNull AppCompatActivity activity) {
        this.activity = activity;

        UserSession session = new UserSession(activity);
        this.primaryEmail = session.getPrimaryAccountEmail();

        if (primaryEmail == null) {
            throw new IllegalStateException("Primary email is null");
        }

        this.cache = session.getVaultCache().trustedAccounts;
        this.driveHelper =
                new TrustedAccountsDriveHelper(activity.getApplicationContext());
        this.accountPicker = new AccountPickerHelper(activity);
        this.consentHelper = new DriveConsentHelper(activity);
    }

    /* ==========================================================
     * Storage observation (CACHE-FIRST)
     * ========================================================== */

    public void observeVaultStorage(@NonNull StorageStateListener listener) {
        this.storageListener = listener;

        if (cache.isInitialized()) {
            emitStorageState(cache.getAccountsView());
            return;
        }

        refreshFromDrive();
    }

    private void refreshFromDrive() {
        driveHelper.fetchTrustedAccounts(
                executor,
                new TrustedAccountsDriveHelper.FetchCallback() {
                    @Override
                    public void onResult(List<TrustedAccount> accounts) {
                        cache.initializeFromDrive(accounts);
                        emitStorageState(accounts);
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Storage fetch failed", e);
                    }
                }
        );
    }

    /* ==========================================================
     * Expand vault flow
     * ========================================================== */

    public void launchExpandVault(@NonNull ExpandActionListener listener) {
        this.actionListener = listener;

        accountPicker.launch(new AccountPickerHelper.Callback() {
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
                        cache.addAccount(account);
                        emitStorageState(cache.getAccountsView());
                        succeedAction();
                    }

                    @Override
                    public void onAlreadyExists() {
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
     * Storage aggregation
     * ========================================================== */

    private void emitStorageState(Iterable<TrustedAccount> accounts) {
        if (storageListener == null) return;

        long total = 0L;
        long used = 0L;

        for (TrustedAccount account : accounts) {
            total += account.totalQuota;
            used += account.usedQuota;
        }

        VaultStorageState state = new VaultStorageState(
                (float) (used / BYTES_IN_GB),
                (float) (total / BYTES_IN_GB),
                UNIT_GB
        );

        runOnUi(() -> storageListener.onVaultStorageState(state));
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
        storageListener = null;
        actionListener = null;
    }
}
