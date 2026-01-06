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
    private static final String ROOT_FOLDER_NAME = "VaultSpace";

    private GoogleAccountCredential credential;

    /* ---------------------------------------------------
     * Account Picker
     * --------------------------------------------------- */
    private final ActivityResultLauncher<Intent> accountPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {

                        Log.d(TAG, "Account picker result received");

                        if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                            Log.w(TAG, "Account picker cancelled");
                            return;
                        }

                        String accountName =
                                result.getData().getStringExtra(
                                        android.accounts.AccountManager.KEY_ACCOUNT_NAME
                                );

                        if (accountName == null) {
                            Log.e(TAG, "Account name is null");
                            return;
                        }

                        Log.d(TAG, "Selected account: " + accountName);

                        Account account = new Account(accountName, "com.google");
                        credential.setSelectedAccount(account);

                        findOrCreateRootFolder();
                    }
            );

    /* ---------------------------------------------------
     * Permission (Consent) Launcher
     * --------------------------------------------------- */
    private final ActivityResultLauncher<Intent> consentLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        Log.d(TAG, "Returned from Drive consent screen");
                        if (result.getResultCode() == RESULT_OK) {
                            Log.d(TAG, "Consent granted, retrying Drive operation");
                            findOrCreateRootFolder();
                        } else {
                            Log.w(TAG, "User denied Drive permission");
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        findViewById(R.id.btnSelectPrimaryDrive)
                .setOnClickListener(v -> requestDriveAccess());
    }

    /* ---------------------------------------------------
     * STEP 1: Pick Google account
     * --------------------------------------------------- */
    private void requestDriveAccess() {

        Log.d(TAG, "Initializing GoogleAccountCredential");

        credential =
                GoogleAccountCredential.usingOAuth2(
                        this,
                        Collections.singleton(
                                "https://www.googleapis.com/auth/drive.file"
                        )
                );

        Intent intent = credential.newChooseAccountIntent();
        accountPickerLauncher.launch(intent);
    }

    /* ---------------------------------------------------
     * STEP 2: Find OR create VaultSpace folder
     * --------------------------------------------------- */
    private void findOrCreateRootFolder() {

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Log.d(TAG, "Building Drive client");

                Drive drive =
                        new Drive.Builder(
                                new NetHttpTransport(),
                                GsonFactory.getDefaultInstance(),
                                credential
                        )
                                .setApplicationName("VaultSpace")
                                .build();

                // 1️⃣ Check if folder already exists
                String query =
                        "name = '" + ROOT_FOLDER_NAME + "' " +
                                "and mimeType = 'application/vnd.google-apps.folder' " +
                                "and trashed = false";

                Log.d(TAG, "Searching for existing VaultSpace folder");

                FileList result =
                        drive.files()
                                .list()
                                .setQ(query)
                                .setSpaces("drive")
                                .setFields("files(id, name)")
                                .execute();

                if (result.getFiles() != null && !result.getFiles().isEmpty()) {

                    File existingFolder = result.getFiles().get(0);

                    Log.d(TAG, "Existing folder found");
                    Log.d(TAG, "Folder ID: " + existingFolder.getId());

                    runOnUiThread(() ->
                            Toast.makeText(
                                    this,
                                    "VaultSpace folder already exists",
                                    Toast.LENGTH_LONG
                            ).show()
                    );

                    // 👉 Save folder ID locally later if needed
                    return;
                }

                // 2️⃣ Folder not found → create it
                Log.d(TAG, "No existing folder found, creating new one");

                File folderMeta = new File();
                folderMeta.setName(ROOT_FOLDER_NAME);
                folderMeta.setMimeType("application/vnd.google-apps.folder");

                File folder =
                        drive.files()
                                .create(folderMeta)
                                .setFields("id,name")
                                .execute();

                Log.d(TAG, "Folder created successfully");
                Log.d(TAG, "Folder ID: " + folder.getId());

                runOnUiThread(() ->
                        Toast.makeText(
                                this,
                                "VaultSpace folder created",
                                Toast.LENGTH_LONG
                        ).show()
                );

            } catch (UserRecoverableAuthIOException e) {

                Log.w(TAG, "Drive permission required – launching consent screen");

                runOnUiThread(() ->
                        consentLauncher.launch(e.getIntent())
                );

            } catch (Exception e) {
                Log.e(TAG, "Drive error", e);

                runOnUiThread(() ->
                        Toast.makeText(
                                this,
                                "Drive error: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
            }
        });
    }
}
