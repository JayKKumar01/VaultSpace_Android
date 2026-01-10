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
import com.github.jaykkumar01.vaultspace.core.session.VaultSessionCache;
import com.github.jaykkumar01.vaultspace.models.TrustedAccount;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.PermissionList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public final class TrustedAccountsDriveHelper {

    private static final String TAG = "VaultSpace:TrustedAccountsDrive";

    private final Drive primaryDrive;
    private final String primaryEmail;
    private final VaultSessionCache cache;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Context appContext;

    /* ==========================================================
     * Callbacks
     * ========================================================== */

    public interface FetchCallback {
        void onResult(List<TrustedAccount> accounts);
        void onError(Exception e);
    }

    public interface AddCallback {
        void onAdded();
        void onAlreadyExists();
        void onError(Exception e);
    }

    /* ==========================================================
     * Constructor
     * ========================================================== */

    public TrustedAccountsDriveHelper(Context context) {
        UserSession session = new UserSession(context);
        this.appContext = context.getApplicationContext();
        this.cache = session.getVaultCache();
        this.primaryEmail = session.getPrimaryAccountEmail();
        this.primaryDrive = DriveClientProvider.forAccount(appContext, primaryEmail);

        Log.d(TAG, "Initialized for primary: " + primaryEmail);
    }

    /* ==========================================================
     * Fetch (cache-first)
     * ========================================================== */

    public void fetchTrustedAccounts(
            ExecutorService executor,
            FetchCallback callback
    ) {
        executor.execute(() -> {
            try {
                if (cache.trustedAccounts.isCached()) {
                    Log.d(TAG, "Trusted accounts cache HIT");
                    postResult(callback, cache.trustedAccounts.get());
                    return;
                }

                Log.d(TAG, "Trusted accounts cache MISS");

                String rootFolderId =
                        DriveFolderRepository.findRootFolderId(primaryDrive);

                if (rootFolderId == null) {
                    cache.trustedAccounts.set(new ArrayList<>());
                    postResult(callback, cache.trustedAccounts.get());
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

                cache.trustedAccounts.set(result);
                postResult(callback, result);

            } catch (Exception e) {
                Log.e(TAG, "Failed to fetch trusted accounts", e);
                postError(callback, e);
            }
        });
    }

    /* ==========================================================
     * Add trusted account
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

                // Incremental cache update
                Drive drive =
                        DriveClientProvider.forAccount(appContext, trustedEmail);

                cache.trustedAccounts.addAccount(
                        DriveStorageRepository.fetchStorageInfo(drive, trustedEmail)
                );

                postAdded(callback);

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

    private void postAdded(AddCallback cb) {
        mainHandler.post(cb::onAdded);
    }

    private void postAlreadyExists(AddCallback cb) {
        mainHandler.post(cb::onAlreadyExists);
    }

    private void postError(AddCallback cb, Exception e) {
        mainHandler.post(() -> cb.onError(e));
    }
}
