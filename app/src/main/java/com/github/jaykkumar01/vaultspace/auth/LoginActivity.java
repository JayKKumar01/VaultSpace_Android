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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpaceDrive";
    private static final String ROOT_FOLDER = "VaultSpace";

    // 🔒 Temporary hardcoded account
    private static final String HARDCODED_EMAIL = "jaytechbc@gmail.com";

    private GoogleAccountCredential credential;

    /* ---------------- Consent Launcher ---------------- */

    private final ActivityResultLauncher<Intent> consentLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            Log.d(TAG, "Drive consent granted");
                            toast("Drive permission granted");
                        } else {
                            Log.w(TAG, "Drive consent denied");
                            toast("Drive permission denied");
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Log.d(TAG, "Initializing Drive credential");

        credential = GoogleAccountCredential.usingOAuth2(
                this,
                Collections.singleton("https://www.googleapis.com/auth/drive.file")
        );

        Account account = new Account(HARDCODED_EMAIL, "com.google");
        credential.setSelectedAccount(account);

        Log.d(TAG, "Using account: " + HARDCODED_EMAIL);

        findViewById(R.id.btnGrantDriveConsent)
                .setOnClickListener(v -> requestConsent());

        findViewById(R.id.btnCreateVaultSpaceFolder)
                .setOnClickListener(v -> createOrOpenFolderAndUploadFile());
    }

    /* ---------------- CONSENT FLOW ---------------- */

    private void requestConsent() {

        Log.d(TAG, "Checking Drive consent");

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Drive drive = buildDrive();

                // Lightweight call just to trigger auth
                drive.about()
                        .get()
                        .setFields("user")
                        .execute();

                Log.d(TAG, "Consent already granted");
                toast("Drive access already granted");

            } catch (UserRecoverableAuthIOException e) {
                Log.d(TAG, "Consent required, launching consent screen");
                runOnUiThread(() -> consentLauncher.launch(e.getIntent()));
            } catch (Exception e) {
                Log.e(TAG, "Consent check failed", e);
                toast("Consent check failed");
            }
        });
    }

    /* ---------------- DRIVE OPERATION ---------------- */

    private void createOrOpenFolderAndUploadFile() {

        Log.d(TAG, "Drive operation requested");

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Drive drive = buildDrive();

                // 1️⃣ Find existing folder
                FileList list =
                        drive.files().list()
                                .setQ(
                                        "name='" + ROOT_FOLDER + "' " +
                                                "and mimeType='application/vnd.google-apps.folder' " +
                                                "and trashed=false"
                                )
                                .setFields("files(id,name)")
                                .execute();

                String folderId;

                if (list.getFiles() != null && !list.getFiles().isEmpty()) {
                    folderId = list.getFiles().get(0).getId();
                    Log.d(TAG, "Folder exists, ID = " + folderId);
                } else {
                    // 2️⃣ Create folder
                    Log.d(TAG, "Folder not found, creating new one");

                    File folder = new File();
                    folder.setName(ROOT_FOLDER);
                    folder.setMimeType("application/vnd.google-apps.folder");

                    File created =
                            drive.files()
                                    .create(folder)
                                    .setFields("id")
                                    .execute();

                    folderId = created.getId();
                    Log.d(TAG, "Folder created, ID = " + folderId);
                }

                // 3️⃣ Always upload a new JSON file
                uploadJsonFile(drive, folderId);

                toast("VaultSpace file uploaded");

            } catch (UserRecoverableAuthIOException e) {
                Log.w(TAG, "Drive consent missing");
                toast("Grant Drive access first");
            } catch (Exception e) {
                Log.e(TAG, "Drive error", e);
                toast("Drive error");
            }
        });
    }

    /* ---------------- JSON UPLOAD ---------------- */

    private void uploadJsonFile(Drive drive, String folderId) throws Exception {

        // ---------- Time ----------
        Date now = new Date();

        SimpleDateFormat isoFmt =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);

        SimpleDateFormat dateFmt =
                new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        SimpleDateFormat timeFmt =
                new SimpleDateFormat("HH:mm:ss", Locale.US);

        String iso = isoFmt.format(now);
        String date = dateFmt.format(now);
        String time = timeFmt.format(now);
        String timezone = TimeZone.getDefault().getID();

        // ---------- Build model ----------
        VaultSpaceMeta meta = new VaultSpaceMeta();
        meta.app = "VaultSpace";
        meta.account = HARDCODED_EMAIL;
        meta.event = "vaultspace_folder_action";
        meta.note = "Triggered from Android app button";

        VaultSpaceMeta.CreatedAt createdAt =
                new VaultSpaceMeta.CreatedAt();

        createdAt.iso = iso;
        createdAt.date = date;
        createdAt.time = time;
        createdAt.timezone = timezone;

        meta.created_at = createdAt;

        // ---------- Serialize (pretty) ----------
        Gson gson =
                new GsonBuilder()
                        .setPrettyPrinting()
                        .create();

        String json = gson.toJson(meta);


        // ---------- File name ----------
        String fileName =
                "vaultspace_" + date + "_" + time.replace(":", "-") + ".json";

        Log.d(TAG, "Uploading JSON file: " + fileName);
        Log.d(TAG, "JSON content:\n" + json);

        // ---------- Drive metadata ----------
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
                        .setFields("id,name")
                        .execute();

        Log.d(TAG, "JSON uploaded successfully");
        Log.d(TAG, "File ID = " + uploaded.getId());
    }


    /* ---------------- Helpers ---------------- */

    private Drive buildDrive() {
        return new Drive.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
        )
                .setApplicationName("VaultSpace")
                .build();
    }

    private void toast(String msg) {
        runOnUiThread(() ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        );
    }
}
