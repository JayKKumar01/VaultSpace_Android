package com.github.jaykkumar01.vaultspace.core.auth;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.util.Collections;

public class DriveConsentHelper {

    private static final String TAG = "VaultSpace:DriveConsent";

    public interface Callback {
        void onConsentGranted();
        void onConsentDenied();
        void onFailure(Exception e);
    }

    private final AppCompatActivity activity;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ActivityResultLauncher<Intent> consentLauncher;

    private final Callback callback;

    /* ---------------- Constructor ---------------- */

    public DriveConsentHelper(AppCompatActivity activity, Callback callback) {
        this.activity = activity;
        this.callback = callback;

        this.consentLauncher =
                activity.registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                                Log.d(TAG, "Consent granted from UI");
                                callback.onConsentGranted();
                            } else {
                                Log.w(TAG, "Consent denied by user");
                                callback.onConsentDenied();
                            }
                        }
                );
    }

    /* ---------------- Public API (LIKE AccountPickerHelper) ---------------- */

    public void launch(String email) {
        Log.d(TAG, "launch() → checking Drive consent for " + email);

        new Thread(() -> {
            try {
                // ✅ Credential created INSIDE helper
                GoogleAccountCredential credential =
                        GoogleAccountCredential.usingOAuth2(
                                activity.getApplicationContext(),
                                Collections.singleton(DriveScopes.DRIVE_FILE)
                        );
                credential.setSelectedAccountName(email);

                Drive drive =
                        new Drive.Builder(
                                new NetHttpTransport(),
                                GsonFactory.getDefaultInstance(),
                                credential
                        )
                                .setApplicationName("VaultSpace")
                                .build();

                // Lightweight call → forces token / consent
                drive.about().get().setFields("user").execute();

                Log.d(TAG, "Drive consent already granted");
                mainHandler.post(callback::onConsentGranted);

            } catch (UserRecoverableAuthIOException e) {
                Log.w(TAG, "Drive consent required");
                mainHandler.post(() ->
                        consentLauncher.launch(e.getIntent())
                );

            } catch (Exception e) {
                Log.e(TAG, "Drive consent check failed", e);
                mainHandler.post(() ->
                        callback.onFailure(e)
                );
            }
        }).start();
    }
}
