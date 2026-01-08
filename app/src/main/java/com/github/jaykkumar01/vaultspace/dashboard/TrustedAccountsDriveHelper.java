package com.github.jaykkumar01.vaultspace.dashboard;

import android.content.Context;
import android.util.Log;

import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.github.jaykkumar01.vaultspace.core.drive.DriveFolderRepository;
import com.github.jaykkumar01.vaultspace.core.drive.DrivePermissionRepository;
import com.github.jaykkumar01.vaultspace.core.drive.DriveStorageRepository;
import com.github.jaykkumar01.vaultspace.models.TrustedAccount;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.PermissionList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrustedAccountsDriveHelper {

    private static final String TAG = "VaultSpace:TrustedAccountsHelper";

    public interface AddResultCallback {
        void onAdded();
        void onAlreadyExists();
        void onFailure(Exception e);
    }

    private final Context appContext;
    private final Drive primaryDrive;
    private final String primaryEmail;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public TrustedAccountsDriveHelper(Context context, String primaryEmail) {
        this.appContext = context.getApplicationContext();
        this.primaryEmail = primaryEmail;
        this.primaryDrive = DriveClientProvider.forAccount(appContext, primaryEmail);
    }

    /* ---------------- Write Path ---------------- */

    public void addTrustedAccountAsync(String trustedEmail, AddResultCallback callback) {
        executor.execute(() -> {
            try {
                String rootFolderId =
                        DriveFolderRepository.getOrCreateRootFolder(primaryDrive);

                if (DrivePermissionRepository.hasWriterAccess(
                        primaryDrive,
                        rootFolderId,
                        trustedEmail
                )) {
                    callback.onAlreadyExists();
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

                callback.onAdded();

            } catch (Exception e) {
                Log.e(TAG, "Failed to add trusted account", e);
                callback.onFailure(e);
            }
        });
    }

    /* ---------------- Read Path ---------------- */

    public List<TrustedAccount> getTrustedAccounts() throws Exception {

        String rootFolderId =
                DriveFolderRepository.findRootFolderId(primaryDrive);

        if (rootFolderId == null) {
            return new ArrayList<>(); // no mutation
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
            if (email == null || email.equalsIgnoreCase(primaryEmail)) continue;

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

        return result;
    }
}
