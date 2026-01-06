package com.github.jaykkumar01.vaultspace.auth;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.models.VaultSpaceMeta;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.gson.GsonBuilder;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpaceDrive";
    private static final String ROOT_FOLDER = "VaultSpace";

    // 🔒 Temporary (intentional)
    private static final String HARDCODED_EMAIL = "jaytechbc@gmail.com";
    private static final String SHARE_WITH_EMAIL = "jaykkumar08@gmail.com";


    private GoogleAccountCredential credential;

    // ✅ Cached Drive client (invalidate on auth failure)
    private Drive drive;

    /* ---------------------------------------------------
     * Consent Launcher
     * --------------------------------------------------- */
    private final ActivityResultLauncher<Intent> consentLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        Log.d(TAG, "Returned from Drive consent screen");
                        if (result.getResultCode() == RESULT_OK) {
                            Log.d(TAG, "✅ Drive consent granted by user");
                            toast("Drive permission granted");
                        } else {
                            Log.w(TAG, "❌ Drive consent denied by user");
                            toast("Drive permission denied");
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Log.d(TAG, "onCreate() → initializing credential");

        credential =
                GoogleAccountCredential.usingOAuth2(
                        this,
                        Collections.singleton("https://www.googleapis.com/auth/drive.file")
                );

        credential.setSelectedAccount(
                new Account(HARDCODED_EMAIL, "com.google")
        );

        Log.d(TAG, "Selected account = " + HARDCODED_EMAIL);

        findViewById(R.id.btnGrantDriveConsent)
                .setOnClickListener(v -> {
                    Log.d(TAG, "Consent button clicked");
                    requestConsent();
                });

        findViewById(R.id.btnCreateVaultSpaceFolder)
                .setOnClickListener(v -> {
                    Log.d(TAG, "Drive operation button clicked");
                    createOrOpenFolderAndUpload();
                });

        findViewById(R.id.btnShareVaultSpaceFolder)
                .setOnClickListener(v -> {
                    Log.d(TAG, "Share folder button clicked");
                    shareVaultSpaceFolder();
                });

    }

    /* ---------------------------------------------------
     * CONSENT CHECK
     * --------------------------------------------------- */
    private void requestConsent() {

        Log.d(TAG, "Checking Drive consent status");

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Drive d = getDrive();

                Log.d(TAG, "Triggering lightweight Drive call for auth check");
                d.about().get().setFields("user").execute();

                Log.d(TAG, "✅ Drive consent already granted");
                toast("Drive access already granted");

            } catch (UserRecoverableAuthIOException e) {
                Log.w(TAG, "⚠ Consent required → launching consent UI");
                runOnUiThread(() -> consentLauncher.launch(e.getIntent()));
            } catch (Exception e) {
                Log.e(TAG, "❌ Consent check failed unexpectedly", e);
                toast("Consent check failed");
            }
        });
    }

    /* ---------------------------------------------------
     * MAIN DRIVE FLOW
     * --------------------------------------------------- */
    private void createOrOpenFolderAndUpload() {

        Log.d(TAG, "Starting Drive operation flow");

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Drive d = getDrive();

                Log.d(TAG, "Finding or creating VaultSpace folder");
                String folderId = findOrCreateFolder(d);

                Log.d(TAG, "Uploading JSON metadata file");
                uploadJsonFile(d, folderId);

                Log.d(TAG, "✅ Drive operation completed");
                toast("VaultSpace file uploaded");

            } catch (UserRecoverableAuthIOException e) {
                Log.w(TAG, "⚠ Auth expired / revoked → invalidating Drive");
                invalidateDrive();
                toast("Grant Drive access first");
            } catch (Exception e) {
                Log.e(TAG, "❌ Drive operation failed", e);
                toast("Drive error");
            }
        });
    }

    /* ---------------------------------------------------
     * FOLDER HANDLING
     * --------------------------------------------------- */
    private String findOrCreateFolder(Drive d) throws Exception {

        Log.d(TAG, "Searching for existing folder: " + ROOT_FOLDER);

        FileList list =
                d.files().list()
                        .setQ(
                                "name='" + ROOT_FOLDER + "' " +
                                        "and mimeType='application/vnd.google-apps.folder' " +
                                        "and trashed=false"
                        )
                        .setFields("files(id,name)")
                        .execute();

        if (list.getFiles() != null && !list.getFiles().isEmpty()) {
            String id = list.getFiles().get(0).getId();
            Log.d(TAG, "✔ Existing folder found, ID = " + id);
            return id;
        }

        Log.d(TAG, "Folder not found → creating new folder");

        File folder = new File();
        folder.setName(ROOT_FOLDER);
        folder.setMimeType("application/vnd.google-apps.folder");

        File created =
                d.files()
                        .create(folder)
                        .setFields("id")
                        .execute();

        Log.d(TAG, "✔ Folder created, ID = " + created.getId());
        return created.getId();
    }

    /* ---------------------------------------------------
     * JSON UPLOAD
     * --------------------------------------------------- */
    private void uploadJsonFile(Drive drive, String folderId) throws Exception {

        Date now = new Date();

        SimpleDateFormat isoFmt =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);

        VaultSpaceMeta meta = new VaultSpaceMeta();
        meta.app = "VaultSpace";
        meta.account = HARDCODED_EMAIL;
        meta.event = "upload";
        meta.note = "Manual trigger";

        VaultSpaceMeta.CreatedAt createdAt =
                new VaultSpaceMeta.CreatedAt();

        createdAt.iso = isoFmt.format(now);
        createdAt.timezone = TimeZone.getDefault().getID();

        meta.created_at = createdAt;

        String json =
                new GsonBuilder()
                        .setPrettyPrinting()
                        .create()
                        .toJson(meta);

        String fileName =
                "vaultspace_" + System.currentTimeMillis() + ".json";

        Log.d(TAG, "Uploading JSON file: " + fileName);
        Log.d(TAG, "JSON content:\n" + json);

        File metadata = new File();
        metadata.setName(fileName);
        metadata.setMimeType("application/json");
        metadata.setParents(Collections.singletonList(folderId));

        ByteArrayContent content =
                new ByteArrayContent(
                        "application/json",
                        json.getBytes(StandardCharsets.UTF_8)
                );

        File uploaded =
                drive.files()
                        .create(metadata, content)
                        .setFields("id")
                        .execute();

        Log.d(TAG, "✔ JSON uploaded, file ID = " + uploaded.getId());
    }

    /* ---------------------------------------------------
     * DRIVE MANAGEMENT
     * --------------------------------------------------- */
    private synchronized Drive getDrive() {

        if (drive == null) {
            Log.d(TAG, "Building new Drive client");
            drive =
                    new Drive.Builder(
                            new NetHttpTransport(),
                            GsonFactory.getDefaultInstance(),
                            credential
                    )
                            .setApplicationName("VaultSpace")
                            .build();
        } else {
            Log.d(TAG, "Reusing cached Drive client");
        }

        return drive;
    }

    private void shareVaultSpaceFolder() {

        Log.d(TAG, "Starting folder sharing flow");

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Drive d = getDrive();

                Log.d(TAG, "Finding VaultSpace folder to share");
                String folderId = findOrCreateFolder(d);

                Log.d(TAG, "Sharing folder with: " + SHARE_WITH_EMAIL);
                shareFolderWithUser(d, folderId, SHARE_WITH_EMAIL);

                toast("Folder shared with " + SHARE_WITH_EMAIL);

            } catch (UserRecoverableAuthIOException e) {
                Log.w(TAG, "Auth required before sharing");
                invalidateDrive();
                toast("Grant Drive access first");

            } catch (Exception e) {
                Log.e(TAG, "Folder sharing failed", e);
                toast("Failed to share folder");
            }
        });
    }


    private void shareFolderWithUser(
            Drive drive,
            String folderId,
            String targetEmail
    ) throws Exception {

        Log.d(TAG, "Sharing folder with user: " + targetEmail);
        Log.d(TAG, "Folder ID: " + folderId);

        com.google.api.services.drive.model.Permission permission =
                new com.google.api.services.drive.model.Permission();

        permission.setType("user");          // user | group | domain | anyone
        permission.setRole("writer");        // reader | commenter | writer | owner
        permission.setEmailAddress(targetEmail);

        drive.permissions()
                .create(folderId, permission)
                .setSendNotificationEmail(true)
                .execute();

        Log.d(TAG, "✅ Folder shared successfully with " + targetEmail);
    }


    private synchronized void invalidateDrive() {
        Log.w(TAG, "Invalidating cached Drive client");
        drive = null;
    }

    private void toast(String msg) {
        runOnUiThread(() ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        );
    }
}
