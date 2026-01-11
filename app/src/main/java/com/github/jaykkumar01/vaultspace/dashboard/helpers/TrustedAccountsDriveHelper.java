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
import com.google.api.services.drive.model.PermissionList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * TrustedAccountsDriveHelper
 *
 * Responsibilities:
 * - Fetch trusted accounts from Drive
 * - Add trusted account permissions
 *
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
        void onResult(List<TrustedAccount> accounts);
        void onError(Exception e);
    }

    public interface AddCallback {
        void onAdded(TrustedAccount account);
        void onAlreadyExists();
        void onError(Exception e);
    }

    /* ==========================================================
     * Constructor
     * ========================================================== */

    public TrustedAccountsDriveHelper(Context context) {
        UserSession session = new UserSession(context);
        this.appContext = context.getApplicationContext();
        this.primaryEmail = session.getPrimaryAccountEmail();
        this.primaryDrive =
                DriveClientProvider.forAccount(appContext, primaryEmail);

        Log.d(TAG, "Initialized for primary: " + primaryEmail);
    }

    /* ==========================================================
     * Fetch (ALWAYS hits Drive)
     * ========================================================== */

    public void fetchTrustedAccounts(
            ExecutorService executor,
            FetchCallback callback
    ) {
        executor.execute(() -> {
            try {
                String rootFolderId =
                        DriveFolderRepository.findRootFolderId(primaryDrive);

                if (rootFolderId == null) {
                    postResult(callback, List.of());
                    return;
                }

                PermissionList permissions =
                        primaryDrive.permissions()
                                .list(rootFolderId)
                                .setFields("permissions(emailAddress,role,type)")
                                .execute();

                List<TrustedAccount> result = new ArrayList<>();

                for (Permission p : permissions.getPermissions()) {

                    if (!"user".equals(p.getType())) continue;
                    if (!"writer".equals(p.getRole())) continue;

                    String email = p.getEmailAddress();
                    if (email == null || email.equalsIgnoreCase(primaryEmail))
                        continue;

                    try {
                        Drive drive =
                                DriveClientProvider.forAccount(appContext, email);

                        result.add(
                                DriveStorageRepository.fetchStorageInfo(drive, email)
                        );

                    } catch (Exception e) {
                        Log.w(TAG, "Skipping trusted account: " + email, e);
                    }
                }

                postResult(callback, result);

            } catch (Exception e) {
                Log.e(TAG, "Failed to fetch trusted accounts", e);
                postError(callback, e);
            }
        });
    }

    /* ==========================================================
     * Add trusted account (Drive-only)
     * ========================================================== */

    public void addTrustedAccount(
            ExecutorService executor,
            String trustedEmail,
            AddCallback callback
    ) {
        executor.execute(() -> {
            try {
                String rootFolderId =
                        DriveFolderRepository.getOrCreateRootFolder(primaryDrive);

                if (DrivePermissionRepository.hasWriterAccess(
                        primaryDrive,
                        rootFolderId,
                        trustedEmail
                )) {
                    postAlreadyExists(callback);
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

                Drive drive =
                        DriveClientProvider.forAccount(appContext, trustedEmail);

                TrustedAccount account =
                        DriveStorageRepository.fetchStorageInfo(drive, trustedEmail);

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

    private void postResult(FetchCallback cb, List<TrustedAccount> accounts) {
        mainHandler.post(() -> cb.onResult(accounts));
    }

    private void postError(FetchCallback cb, Exception e) {
        mainHandler.post(() -> cb.onError(e));
    }

    private void postAdded(AddCallback cb, TrustedAccount account) {
        mainHandler.post(() -> cb.onAdded(account));
    }

    private void postAlreadyExists(AddCallback cb) {
        mainHandler.post(cb::onAlreadyExists);
    }

    private void postError(AddCallback cb, Exception e) {
        mainHandler.post(() -> cb.onError(e));
    }
}
