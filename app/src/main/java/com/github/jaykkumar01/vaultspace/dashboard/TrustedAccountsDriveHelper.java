package com.github.jaykkumar01.vaultspace.dashboard;

import android.accounts.Account;
import android.content.Context;
import android.util.Log;

import com.github.jaykkumar01.vaultspace.core.auth.DriveConsentFlowHelper;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TrustedAccountsDriveHelper {

    private static final String TAG = "VaultSpace:TrustedAccountsHelper";
    private static final String ROOT_FOLDER_NAME = "VaultSpace";
    private static final String FILE_NAME = "trusted_accounts.json";

    private final Drive drive;
    private final Gson gson = new Gson();

    public TrustedAccountsDriveHelper(Context context, String primaryEmail) {

        // ✅ Local credential instance (NO shared mutation)
        GoogleAccountCredential localCredential =
                DriveConsentFlowHelper.createCredential(context);

        localCredential.setSelectedAccount(
                new Account(primaryEmail, "com.google")
        );

        drive = new Drive.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                localCredential
        ).setApplicationName("VaultSpace").build();
    }

    /* ---------------- Public API ---------------- */

    public void addTrustedAccount(String trustedEmail) throws Exception {
        String rootFolderId = getOrCreateRootFolder();
        File file = findTrustedAccountsFile(rootFolderId);

        List<String> accounts =
                file == null ? new ArrayList<>() : readAccounts(file.getId());

        if (accounts.contains(trustedEmail)) {
            Log.d(TAG, "Trusted account already exists: " + trustedEmail);
            return;
        }

        accounts.add(trustedEmail);
        String json = gson.toJson(accounts);

        if (file == null) {
            createFile(rootFolderId, json);
        } else {
            updateFile(file.getId(), json);
        }

        Log.d(TAG, "Trusted account added: " + trustedEmail);
    }

    /* ---------------- Internal ---------------- */

    private String getOrCreateRootFolder() throws Exception {
        FileList list = drive.files().list()
                .setQ("mimeType='application/vnd.google-apps.folder' and name='" +
                        ROOT_FOLDER_NAME + "' and trashed=false")
                .setSpaces("drive")
                .execute();

        if (!list.getFiles().isEmpty()) {
            return list.getFiles().get(0).getId();
        }

        File folder = new File();
        folder.setName(ROOT_FOLDER_NAME);
        folder.setMimeType("application/vnd.google-apps.folder");

        return drive.files().create(folder).execute().getId();
    }

    private File findTrustedAccountsFile(String parentId) throws Exception {
        FileList list = drive.files().list()
                .setQ("name='" + FILE_NAME + "' and '" + parentId +
                        "' in parents and trashed=false")
                .setSpaces("drive")
                .execute();

        return list.getFiles().isEmpty() ? null : list.getFiles().get(0);
    }

    private List<String> readAccounts(String fileId) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        drive.files().get(fileId).executeMediaAndDownloadTo(out);

        Type type = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(
                out.toString(StandardCharsets.UTF_8.name()),
                type
        );
    }

    private void createFile(String parentId, String json) throws Exception {
        File meta = new File();
        meta.setName(FILE_NAME);
        meta.setParents(List.of(parentId));

        drive.files().create(
                meta,
                new ByteArrayContent(
                        "application/json",
                        json.getBytes(StandardCharsets.UTF_8)
                )
        ).execute();
    }

    private void updateFile(String fileId, String json) throws Exception {
        drive.files().update(
                fileId,
                null,
                new ByteArrayContent(
                        "application/json",
                        json.getBytes(StandardCharsets.UTF_8)
                )
        ).execute();
    }
}
