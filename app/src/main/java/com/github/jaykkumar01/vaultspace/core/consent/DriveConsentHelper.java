package com.github.jaykkumar01.vaultspace.core.consent;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.core.auth.GoogleCredentialFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DriveConsentHelper {

    private static final String TAG = "VaultSpace:DriveConsent";

    public interface Callback {
        void onConsentGranted(String email);
        void onConsentDenied(String email);
        void onFailure(String email, Exception e);
    }

    private final AppCompatActivity activity;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent> consentLauncher;

    private Callback callback;
    private String pendingEmail;

    /* ---------------- Constructor ---------------- */

    public DriveConsentHelper(AppCompatActivity activity) {
        this.activity = activity;

        this.consentLauncher =
                activity.registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (callback == null || pendingEmail == null) return;

                            if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                                Log.d(TAG, "Consent granted from UI");
                                callback.onConsentGranted(pendingEmail);
                            } else {
                                Log.w(TAG, "Consent denied by user");
                                callback.onConsentDenied(pendingEmail);
                            }

                            pendingEmail = null;
                        }
                );
    }

    /* ---------------- Public API ---------------- */

    public void launch(String email, Callback callback) {
        this.callback = callback;
        this.pendingEmail = email;

        Log.d(TAG, "Checking Drive consent for " + email);

        executor.execute(() -> {
            try {
                GoogleAccountCredential credential =
                        GoogleCredentialFactory.forDrive(activity, email);

                Drive drive =
                        new Drive.Builder(
                                new NetHttpTransport(),
                                GsonFactory.getDefaultInstance(),
                                credential
                        )
                                .setApplicationName("VaultSpace")
                                .build();

                drive.about().get().setFields("user").execute();

                Log.d(TAG, "Drive consent already granted");
                mainHandler.post(() ->
                        callback.onConsentGranted(email)
                );

            } catch (UserRecoverableAuthIOException e) {
                Log.w(TAG, "Drive consent required");
                mainHandler.post(() ->
                        consentLauncher.launch(e.getIntent())
                );

            } catch (Exception e) {
                Log.e(TAG, "Drive consent check failed", e);
                mainHandler.post(() ->
                        callback.onFailure(email, e)
                );
            }
        });
    }
}
