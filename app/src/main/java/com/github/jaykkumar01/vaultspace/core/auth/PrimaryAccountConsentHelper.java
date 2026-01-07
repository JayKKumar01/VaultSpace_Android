package com.github.jaykkumar01.vaultspace.core.auth;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PrimaryAccountConsentHelper {

    private static final String TAG = "VaultSpace:PrimaryConsent";

    public interface Callback {
        void onAllConsentsGranted();
        void onConsentDenied();
        void onFailure(Exception e);
    }

    private enum ConsentResult {
        GRANTED,
        RECOVERABLE,
        FAILED
    }

    private final AppCompatActivity activity;
    private final Callback callback;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent> consentLauncher;

    private String pendingEmail;
    private boolean consentUiShown = false;

    public PrimaryAccountConsentHelper(
            AppCompatActivity activity,
            Callback callback
    ) {
        this.activity = activity;
        this.callback = callback;

        this.consentLauncher =
                activity.registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            Log.d(TAG, "Consent UI returned");

                            if (pendingEmail == null) {
                                callback.onFailure(
                                        new IllegalStateException("Missing pending email")
                                );
                                return;
                            }

                            // 🚫 DO NOT relaunch UI again
                            verifyAfterUi(pendingEmail);
                        }
                );
    }

    /* ---------------------------------------------------
     * Public API
     * --------------------------------------------------- */

    public void launch(String email) {
        Log.d(TAG, "launch() → starting consent flow for " + email);

        this.pendingEmail = email;
        this.consentUiShown = false;

        runInitialCheck(email);
    }

    /* ---------------------------------------------------
     * Flow Control
     * --------------------------------------------------- */

    private void runInitialCheck(String email) {
        executor.execute(() -> {
            ConsentResult result = checkBothConsents(email);

            mainHandler.post(() -> {
                switch (result) {
                    case GRANTED:
                        callback.onAllConsentsGranted();
                        break;

                    case RECOVERABLE:
                        if (!consentUiShown) {
                            consentUiShown = true;
                            Log.w(TAG, "Launching consent UI");
                            consentLauncher.launch(lastRecoverableIntent);
                        } else {
                            Log.w(TAG, "Consent denied after UI");
                            callback.onConsentDenied();
                        }
                        break;

                    case FAILED:
                        callback.onFailure(
                                new IllegalStateException("Consent check failed")
                        );
                        break;
                }
            });
        });
    }

    private void verifyAfterUi(String email) {
        executor.execute(() -> {
            ConsentResult result = checkBothConsents(email);

            mainHandler.post(() -> {
                if (result == ConsentResult.GRANTED) {
                    callback.onAllConsentsGranted();
                } else {
                    Log.w(TAG, "User returned without granting consent");
                    callback.onConsentDenied();
                }
            });
        });
    }

    /* ---------------------------------------------------
     * Core Utility (Reusable & Testable)
     * --------------------------------------------------- */

    private volatile Intent lastRecoverableIntent;

    private ConsentResult checkBothConsents(String email) {
        try {
            GoogleAccountCredential credential =
                    GoogleAccountCredential.usingOAuth2(
                            activity.getApplicationContext(),
                            Arrays.asList(
                                    "https://www.googleapis.com/auth/drive.file",
                                    "https://www.googleapis.com/auth/userinfo.profile"
                            )
                    );
            credential.setSelectedAccountName(email);

            // Drive probe
            Drive drive =
                    new Drive.Builder(
                            new NetHttpTransport(),
                            GsonFactory.getDefaultInstance(),
                            credential
                    )
                            .setApplicationName("VaultSpace")
                            .build();

            drive.about().get().setFields("user").execute();

            // Profile probe
            credential.getToken();

            Log.d(TAG, "All consents verified");
            return ConsentResult.GRANTED;

        } catch (UserRecoverableAuthException e) {
            Log.w(TAG, "Recoverable consent needed (GMS)");
            lastRecoverableIntent = e.getIntent();
            return ConsentResult.RECOVERABLE;

        } catch (UserRecoverableAuthIOException e) {
            Log.w(TAG, "Recoverable consent needed (HTTP)");
            lastRecoverableIntent = e.getIntent();
            return ConsentResult.RECOVERABLE;

        } catch (Exception e) {
            Log.e(TAG, "Consent validation failed", e);
            return ConsentResult.FAILED;
        }
    }
}
