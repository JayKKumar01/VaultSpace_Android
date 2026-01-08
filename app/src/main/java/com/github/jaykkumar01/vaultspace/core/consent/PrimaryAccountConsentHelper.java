package com.github.jaykkumar01.vaultspace.core.consent;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.core.auth.GoogleCredentialFactory;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class PrimaryAccountConsentHelper {

    private static final String TAG = "VaultSpace:PrimaryConsent";

    /* ---------------- Public Types ---------------- */

    public interface LoginCallback {
        void onAllConsentsGranted();
        void onConsentDenied();
        void onFailure(Exception e);
    }

    public enum ConsentResult {
        GRANTED,
        RECOVERABLE,
        FAILED
    }

    /* ---------------- Core ---------------- */

    private final AppCompatActivity activity;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    /* ---------------- Login-only state ---------------- */

    private LoginCallback loginCallback;
    private String pendingEmail;
    private boolean consentUiShown = false;
    private volatile Intent lastRecoverableIntent;

    private final ActivityResultLauncher<Intent> consentLauncher;

    /* ---------------- Constructor ---------------- */

    public PrimaryAccountConsentHelper(AppCompatActivity activity) {
        this.activity = activity;

        this.consentLauncher =
                activity.registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            Log.d(TAG, "Consent UI returned");

                            if (pendingEmail == null || loginCallback == null) {
                                Log.e(TAG, "Invalid state after consent UI");
                                return;
                            }

                            verifyAfterUi(pendingEmail);
                        }
                );
    }

    /* ===================================================
     * DASHBOARD API (NO UI)
     * =================================================== */

    public void checkConsentsSilently(
            String email,
            Consumer<ConsentResult> resultCallback
    ) {
        executor.execute(() -> {
            ConsentResult result = checkConsentsInternal(email);
            mainHandler.post(() -> resultCallback.accept(result));
        });
    }

    /* ===================================================
     * LOGIN API (UI + SINGLE RETRY)
     * =================================================== */

    public void startLoginConsentFlow(
            String email,
            LoginCallback callback
    ) {
        Log.d(TAG, "Starting login consent flow for " + email);

        this.pendingEmail = email;
        this.loginCallback = callback;
        this.consentUiShown = false;

        executor.execute(() -> {
            ConsentResult result = checkConsentsInternal(email);
            mainHandler.post(() -> handleLoginResult(result));
        });
    }

    private void handleLoginResult(ConsentResult result) {
        switch (result) {
            case GRANTED:
                loginCallback.onAllConsentsGranted();
                break;

            case RECOVERABLE:
                if (!consentUiShown && lastRecoverableIntent != null) {
                    consentUiShown = true;
                    Log.w(TAG, "Launching consent UI");
                    consentLauncher.launch(lastRecoverableIntent);
                } else {
                    Log.w(TAG, "Consent denied after UI");
                    loginCallback.onConsentDenied();
                }
                break;

            case FAILED:
                loginCallback.onFailure(
                        new IllegalStateException("Consent validation failed")
                );
                break;
        }
    }

    private void verifyAfterUi(String email) {
        executor.execute(() -> {
            ConsentResult result = checkConsentsInternal(email);

            mainHandler.post(() -> {
                if (result == ConsentResult.GRANTED) {
                    loginCallback.onAllConsentsGranted();
                } else {
                    loginCallback.onConsentDenied();
                }
            });
        });
    }

    /* ===================================================
     * CORE VALIDATION (PRIVATE)
     * =================================================== */

    private ConsentResult checkConsentsInternal(String email) {
        try {
            GoogleAccountCredential credential = GoogleCredentialFactory.forPrimaryAccount(activity, email);

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
            lastRecoverableIntent = e.getIntent();
            return ConsentResult.RECOVERABLE;

        } catch (UserRecoverableAuthIOException e) {
            lastRecoverableIntent = e.getIntent();
            return ConsentResult.RECOVERABLE;

        } catch (Exception e) {
            Log.e(TAG, "Consent validation failed", e);
            return ConsentResult.FAILED;
        }
    }
}
