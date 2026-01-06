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

    private GoogleAccountCredential credential;
    private Account selectedAccount;

    /* ---------------- Account Picker ---------------- */

    private final ActivityResultLauncher<Intent> accountPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                            Log.w(TAG, "Account picker cancelled");
                            return;
                        }

                        String name = result.getData()
                                .getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME);

                        if (name == null) return;

                        selectedAccount = new Account(name, "com.google");
                        credential.setSelectedAccount(selectedAccount);

                        Log.d(TAG, "Account selected: " + name);
                        toast("Account selected");
                    }
            );

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
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        credential = GoogleAccountCredential.usingOAuth2(
                this,
                Collections.singleton("https://www.googleapis.com/auth/drive.file")
        );

        findViewById(R.id.btnSelectAccount)
                .setOnClickListener(v -> pickAccount());

        findViewById(R.id.btnGrantDriveConsent)
                .setOnClickListener(v -> requestConsent());

        findViewById(R.id.btnCreateVaultSpaceFolder)
                .setOnClickListener(v -> createOrOpenFolder());
    }

    /* ---------------- Actions ---------------- */

    private void pickAccount() {
        Log.d(TAG, "Selecting Google account");
        accountPickerLauncher.launch(credential.newChooseAccountIntent());
    }

    private void requestConsent() {

        if (selectedAccount == null) {
            toast("Select account first");
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Drive drive = buildDrive();
                drive.about().get().setFields("user").execute();
                Log.d(TAG, "Consent already granted");
                toast("Consent already granted");
            } catch (UserRecoverableAuthIOException e) {
                Log.d(TAG, "Launching consent screen");
                runOnUiThread(() -> consentLauncher.launch(e.getIntent()));
            } catch (Exception e) {
                Log.e(TAG, "Consent check failed", e);
            }
        });
    }

    private void createOrOpenFolder() {

        if (selectedAccount == null) {
            toast("Select account first");
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Drive drive = buildDrive();

                FileList list = drive.files().list()
                        .setQ("name='" + ROOT_FOLDER + "' and mimeType='application/vnd.google-apps.folder' and trashed=false")
                        .setFields("files(id,name)")
                        .execute();

                if (!list.getFiles().isEmpty()) {
                    Log.d(TAG, "Folder exists: " + list.getFiles().get(0).getId());
                    toast("VaultSpace folder ready");
                    return;
                }

                File folder = new File();
                folder.setName(ROOT_FOLDER);
                folder.setMimeType("application/vnd.google-apps.folder");

                File created = drive.files().create(folder).setFields("id").execute();
                Log.d(TAG, "Folder created: " + created.getId());
                toast("VaultSpace folder created");

            } catch (UserRecoverableAuthIOException e) {
                Log.w(TAG, "Consent missing");
                toast("Grant Drive access first");
            } catch (Exception e) {
                Log.e(TAG, "Drive error", e);
            }
        });
    }

    private Drive buildDrive() {
        return new Drive.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
        ).setApplicationName("VaultSpace").build();
    }

    private void toast(String msg) {
        runOnUiThread(() ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        );
    }
}
