package com.github.jaykkumar01.vaultspace.dashboard;

import android.accounts.Account;
import android.content.Context;
import android.util.Log;

import com.github.jaykkumar01.vaultspace.core.auth.DriveConsentFlowHelper;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.PermissionList;

import java.util.ArrayList;
import java.util.List;

public class TrustedAccountsDriveHelper {

    private static final String TAG = "VaultSpace:TrustedAccountsHelper";
    private static final String ROOT_FOLDER_NAME = "VaultSpace";

    private final Drive drive;
    private final String primaryEmail;

    public TrustedAccountsDriveHelper(Context context, String primaryEmail) {

        this.primaryEmail = primaryEmail;

        // ✅ Local credential (isolated, safe)
        GoogleAccountCredential credential =
                DriveConsentFlowHelper.createCredential(context);

        credential.setSelectedAccount(
                new Account(primaryEmail, "com.google")
        );

        drive = new Drive.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
        ).setApplicationName("VaultSpace").build();

        Log.d(TAG, "Drive helper initialized for primary: " + primaryEmail);
    }

    /* ---------------------------------------------------
     * PUBLIC API
     * --------------------------------------------------- */

    /**
     * Grants writer access on VaultSpace folder
     */
    public void addTrustedAccount(String trustedEmail) throws Exception {

        String folderId = getOrCreateRootFolder();

        if (hasWriterAccess(folderId, trustedEmail)) {
            Log.d(TAG, "Trusted account already has access: " + trustedEmail);
            return;
        }

        Log.d(TAG, "Granting VaultSpace access to: " + trustedEmail);

        Permission permission = new Permission();
        permission.setType("user");
        permission.setRole("writer");
        permission.setEmailAddress(trustedEmail);

        drive.permissions()
                .create(folderId, permission)
                .setSendNotificationEmail(true)
                .execute();

        Log.d(TAG, "✅ Access granted to trusted account: " + trustedEmail);
    }

    /**
     * Returns list of trusted account emails
     */
    public List<String> getTrustedAccounts() throws Exception {

        String folderId = getOrCreateRootFolder();
        PermissionList permissions =
                drive.permissions()
                        .list(folderId)
                        .setFields("permissions(id,emailAddress,role,type)")
                        .execute();

        List<String> trustedAccounts = new ArrayList<>();

        for (Permission p : permissions.getPermissions()) {

            if (!"user".equals(p.getType())) continue;
            if (!"writer".equals(p.getRole())) continue;

            String email = p.getEmailAddress();
            if (email == null) continue;
            if (email.equalsIgnoreCase(primaryEmail)) continue;

            trustedAccounts.add(email);
        }

        Log.d(TAG, "Trusted accounts count: " + trustedAccounts.size());
        return trustedAccounts;
    }

    /**
     * Revokes VaultSpace access from a trusted account
     */
    public void removeTrustedAccount(String email) throws Exception {

        String folderId = getOrCreateRootFolder();
        PermissionList permissions =
                drive.permissions().list(folderId).execute();

        for (Permission p : permissions.getPermissions()) {
            if ("user".equals(p.getType())
                    && email.equalsIgnoreCase(p.getEmailAddress())) {

                drive.permissions()
                        .delete(folderId, p.getId())
                        .execute();

                Log.d(TAG, "❌ Access revoked for: " + email);
                return;
            }
        }

        Log.w(TAG, "No permission found for: " + email);
    }

    /* ---------------------------------------------------
     * INTERNAL
     * --------------------------------------------------- */

    private boolean hasWriterAccess(String folderId, String email)
            throws Exception {

        Log.d(TAG, "Checking writer access for: " + email);

        PermissionList permissions =
                drive.permissions()
                        .list(folderId)
                        .setFields("permissions(role,emailAddress)")
                        .execute();

        for (Permission p : permissions.getPermissions()) {

            if ("writer".equals(p.getRole())
                    && email.equalsIgnoreCase(p.getEmailAddress())) {

                Log.d(TAG, "✔ Writer access confirmed for " + email);
                return true;
            }
        }

        Log.d(TAG, "✘ Writer access NOT found for " + email);
        return false;
    }

    private String getOrCreateRootFolder() throws Exception {

        FileList list =
                drive.files()
                        .list()
                        .setQ("mimeType='application/vnd.google-apps.folder' and name='"
                                + ROOT_FOLDER_NAME + "' and trashed=false")
                        .setSpaces("drive")
                        .setFields("files(id,name)")
                        .execute();

        if (!list.getFiles().isEmpty()) {
            return list.getFiles().get(0).getId();
        }

        Log.d(TAG, "VaultSpace folder not found, creating new");

        File folder = new File();
        folder.setName(ROOT_FOLDER_NAME);
        folder.setMimeType("application/vnd.google-apps.folder");

        return drive.files()
                .create(folder)
                .setFields("id")
                .execute()
                .getId();
    }
}
