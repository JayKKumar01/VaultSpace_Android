package com.github.jaykkumar01.vaultspace.dashboard.helpers;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.github.jaykkumar01.vaultspace.core.drive.DriveFolderRepository;
import com.github.jaykkumar01.vaultspace.core.drive.DrivePermissionRepository;
import com.github.jaykkumar01.vaultspace.core.drive.DriveStorageRepository;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.models.TrustedAccount;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * TrustedAccountsDriveHelper
 * <p>
 * Responsibilities:
 * - Fetch trusted accounts from Drive
 * - Add trusted account permissions
 * <p>
 * Non-responsibilities:
 * - Caching
 * - UI state
 * - Session lifecycle
 */
public final class TrustedAccountsDriveHelper {

    private static final String TAG = "VaultSpace:TrustedAccountsDrive";

    private final Drive primaryDrive;
    private final String primaryEmail;
    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /* ==========================================================
     * Callbacks
     * ========================================================== */

    public interface FetchCallback {
        void onResult(List<TrustedAccount> accounts, List<String> linkedEmails);

        void onError(Exception e);
    }

    public interface AddCallback {
        void onAdded(TrustedAccount account);

        void onAlreadyExists(TrustedAccount account);

        void onError(Exception e);
    }

    /* ==========================================================
     * Constructor
     * ========================================================== */

    public TrustedAccountsDriveHelper(Context context) {
        UserSession session = new UserSession(context);
        this.appContext = context.getApplicationContext();
        this.primaryEmail = session.getPrimaryAccountEmail();
        this.primaryDrive = DriveClientProvider.getPrimaryDrive(appContext);

        Log.d(TAG, "Initialized for primary: " + primaryEmail);
    }

    /* ==========================================================
     * Fetch (delegated)
     * ========================================================== */

    public void fetchTrustedAccounts(ExecutorService executor, FetchCallback callback) {
        TrustedAccountsFetchWorker.fetch(executor, appContext, primaryDrive, primaryEmail, new TrustedAccountsFetchWorker.Callback() {
                    @Override
                    public void onSuccess(List<TrustedAccount> accounts, List<String> linkedEmails) {
                        postResult(callback, accounts, linkedEmails);
                    }

                    @Override
                    public void onError(Exception e) {
                        postError(callback, e);
                    }
                }
        );
    }

    /* ==========================================================
     * Add trusted account (UNCHANGED)
     * ========================================================== */

    public void addTrustedAccount(ExecutorService executor, String trustedEmail, AddCallback callback) {
        executor.execute(() -> {
            try {
                String rootFolderId = DriveFolderRepository.getRootFolderId(appContext);

                Drive drive;
                TrustedAccount account;

                if (DrivePermissionRepository.hasWriterAccess(primaryDrive, rootFolderId, trustedEmail)) {
                    drive = DriveClientProvider.forAccount(appContext, trustedEmail);
                    account = DriveStorageRepository.fetchStorageInfo(drive, trustedEmail);
                    postAlreadyExists(callback, account);
                    return;
                }

                Permission permission = new Permission()
                        .setType("user")
                        .setRole("writer")
                        .setEmailAddress(trustedEmail);

                primaryDrive.permissions()
                        .create(rootFolderId, permission)
                        .setSendNotificationEmail(true)
                        .execute();

                drive = DriveClientProvider.forAccount(appContext, trustedEmail);

                account = DriveStorageRepository.fetchStorageInfo(drive, trustedEmail);

                postAdded(callback, account);

            } catch (Exception e) {
                Log.e(TAG, "Failed to add trusted account", e);
                postError(callback, e);
            }
        });
    }

    /* ==========================================================
     * Main-thread helpers
     * ========================================================== */

    private void postResult(FetchCallback cb, List<TrustedAccount> accounts, List<String> linkedEmails) {
        mainHandler.post(() -> cb.onResult(accounts,linkedEmails));
    }

    private void postError(FetchCallback cb, Exception e) {
        mainHandler.post(() -> cb.onError(e));
    }

    private void postAdded(AddCallback cb, TrustedAccount account) {
        mainHandler.post(() -> cb.onAdded(account));
    }

    private void postAlreadyExists(AddCallback cb, TrustedAccount account) {
        mainHandler.post(() -> cb.onAlreadyExists(account));
    }

    private void postError(AddCallback cb, Exception e) {
        mainHandler.post(() -> cb.onError(e));
    }
}
