package com.github.jaykkumar01.vaultspace.dashboard;

import android.accounts.Account;
import android.content.Context;
import android.util.Log;

import com.github.jaykkumar01.vaultspace.models.TrustedAccount;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.PermissionList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrustedAccountsDriveHelper {

    private static final String TAG = "VaultSpace:TrustedAccountsHelper";
    private static final String ROOT_FOLDER_NAME = "VaultSpace";
    private static final String DRIVE_SCOPE =
            "https://www.googleapis.com/auth/drive.file";

    private final Context appContext;
    private final Drive primaryDrive;
    private final String primaryEmail;

    public TrustedAccountsDriveHelper(Context context, String primaryEmail) {

        this.appContext = context.getApplicationContext();
        this.primaryEmail = primaryEmail;

        GoogleAccountCredential credential =
                createCredential(primaryEmail);

        this.primaryDrive =
                new Drive.Builder(
                        new NetHttpTransport(),
                        GsonFactory.getDefaultInstance(),
                        credential
                )
                        .setApplicationName("VaultSpace")
                        .build();

        Log.d(TAG, "Drive helper initialized for primary: " + primaryEmail);
    }

    /* ---------------------------------------------------
     * PUBLIC API
     * --------------------------------------------------- */

    /**
     * Grants writer access on VaultSpace root folder
     */
    public void addTrustedAccount(String trustedEmail) throws Exception {

        String folderId = getOrCreateRootFolder();

        if (hasWriterAccess(folderId, trustedEmail)) {
            Log.d(TAG, "Trusted account already has access: " + trustedEmail);
            return;
        }

        Permission permission = new Permission()
                .setType("user")
                .setRole("writer")
                .setEmailAddress(trustedEmail);

        primaryDrive.permissions()
                .create(folderId, permission)
                .setSendNotificationEmail(true)
                .execute();

        Log.d(TAG, "✅ Access granted to trusted account: " + trustedEmail);
    }

    /**
     * Returns trusted accounts with storage quota info.
     * NOTE: Storage quota belongs to the ACCOUNT, not the folder.
     */
    public List<TrustedAccount> getTrustedAccounts() throws Exception {

        String folderId = getOrCreateRootFolder();

        PermissionList permissions =
                primaryDrive.permissions()
                        .list(folderId)
                        .setFields("permissions(emailAddress,role,type)")
                        .execute();

        List<TrustedAccount> result = new ArrayList<>();

        for (Permission p : permissions.getPermissions()) {

            if (!"user".equals(p.getType())) continue;
            if (!"writer".equals(p.getRole())) continue;

            String email = p.getEmailAddress();
            if (email == null) continue;
            if (email.equalsIgnoreCase(primaryEmail)) continue;

            try {
                result.add(fetchStorageInfo(email));
            } catch (Exception e) {
                // Fail-soft: permissions are truth, storage is optional
                Log.w(
                        TAG,
                        "Skipping trusted account (no storage access): " + email,
                        e
                );
            }
        }

        Log.d(TAG, "Trusted accounts with storage info: " + result.size());
        return result;
    }

    /* ---------------------------------------------------
     * INTERNAL HELPERS
     * --------------------------------------------------- */

    private GoogleAccountCredential createCredential(String email) {

        GoogleAccountCredential credential =
                GoogleAccountCredential.usingOAuth2(
                        appContext,
                        Collections.singleton(DRIVE_SCOPE)
                );

        credential.setSelectedAccount(
                new Account(email, "com.google")
        );

        return credential;
    }

    /**
     * Fetches storage quota for a trusted account.
     * Requires prior Drive consent from that account.
     */
    private TrustedAccount fetchStorageInfo(String email) throws Exception {

        Drive drive =
                new Drive.Builder(
                        new NetHttpTransport(),
                        GsonFactory.getDefaultInstance(),
                        createCredential(email)
                )
                        .setApplicationName("VaultSpace")
                        .build();

        About about =
                drive.about()
                        .get()
                        .setFields("storageQuota(limit,usage)")
                        .execute();

        long limit = about.getStorageQuota().getLimit();
        long usage = about.getStorageQuota().getUsage();

        return new TrustedAccount(
                email,
                limit,
                usage,
                Math.max(0, limit - usage)
        );
    }

    private boolean hasWriterAccess(String folderId, String email)
            throws Exception {

        PermissionList permissions =
                primaryDrive.permissions()
                        .list(folderId)
                        .setFields("permissions(role,emailAddress)")
                        .execute();

        for (Permission p : permissions.getPermissions()) {
            if ("writer".equals(p.getRole())
                    && email.equalsIgnoreCase(p.getEmailAddress())) {
                return true;
            }
        }
        return false;
    }

    private String getOrCreateRootFolder() throws Exception {

        FileList list =
                primaryDrive.files()
                        .list()
                        .setQ(
                                "mimeType='application/vnd.google-apps.folder' " +
                                        "and name='" + ROOT_FOLDER_NAME + "' " +
                                        "and trashed=false"
                        )
                        .setSpaces("drive")
                        .setFields("files(id)")
                        .execute();

        if (!list.getFiles().isEmpty()) {
            return list.getFiles().get(0).getId();
        }

        File folder = new File()
                .setName(ROOT_FOLDER_NAME)
                .setMimeType("application/vnd.google-apps.folder");

        return primaryDrive.files()
                .create(folder)
                .setFields("id")
                .execute()
                .getId();
    }
}
