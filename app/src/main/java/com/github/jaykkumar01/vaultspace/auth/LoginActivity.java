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
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.Collections;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpaceDrive";
    private static final String ROOT_FOLDER = "VaultSpace";

    // 🔒 Hardcoded account (temporary, intentional)
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

        Log.d(TAG, "Initializing credential");

        credential = GoogleAccountCredential.usingOAuth2(
                this,
                Collections.singleton("https://www.googleapis.com/auth/drive.file")
        );

        // ✅ Set account directly
        Account selectedAccount = new Account(HARDCODED_EMAIL, "com.google");
        credential.setSelectedAccount(selectedAccount);

        Log.d(TAG, "Hardcoded account set: " + HARDCODED_EMAIL);

        findViewById(R.id.btnGrantDriveConsent)
                .setOnClickListener(v -> requestConsent());

        findViewById(R.id.btnCreateVaultSpaceFolder)
                .setOnClickListener(v -> createOrOpenFolder());
    }

    /* ---------------- CONSENT FLOW ---------------- */

    private void requestConsent() {

        Log.d(TAG, "Checking Drive consent");

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Drive drive = buildDrive();

                // Lightweight call to trigger auth
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

    private void createOrOpenFolder() {

        Log.d(TAG, "Create/Open VaultSpace folder requested");

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Drive drive = buildDrive();

                // 🔍 Check existing folder
                FileList list =
                        drive.files().list()
                                .setQ(
                                        "name='" + ROOT_FOLDER + "' " +
                                                "and mimeType='application/vnd.google-apps.folder' " +
                                                "and trashed=false"
                                )
                                .setFields("files(id,name)")
                                .execute();

                if (list.getFiles() != null && !list.getFiles().isEmpty()) {
                    String id = list.getFiles().get(0).getId();
                    Log.d(TAG, "Folder exists, ID = " + id);
                    toast("VaultSpace folder ready");
                    return;
                }

                // 📁 Create folder
                File folder = new File();
                folder.setName(ROOT_FOLDER);
                folder.setMimeType("application/vnd.google-apps.folder");

                File created =
                        drive.files()
                                .create(folder)
                                .setFields("id")
                                .execute();

                Log.d(TAG, "Folder created, ID = " + created.getId());
                toast("VaultSpace folder created");

            } catch (UserRecoverableAuthIOException e) {
                Log.w(TAG, "Drive consent missing");
                toast("Grant Drive access first");
            } catch (Exception e) {
                Log.e(TAG, "Drive error", e);
                toast("Drive error");
            }
        });
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
