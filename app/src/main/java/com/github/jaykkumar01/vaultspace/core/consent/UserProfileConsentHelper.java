package com.github.jaykkumar01.vaultspace.core.consent;

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

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Ensures Google user profile consent (userinfo.profile).
 *
 * Scope:
 * https://www.googleapis.com/auth/userinfo.profile
 */
public final class UserProfileConsentHelper {

    private static final String TAG = "VaultSpace:ProfileConsent";
    private static final String PROFILE_SCOPE =
            "https://www.googleapis.com/auth/userinfo.profile";

    public interface Callback {
        void onConsentGranted(String email);
        void onConsentDenied(String email);
        void onFailure(String email, Exception e);
    }

    private final AppCompatActivity activity;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent> consentLauncher;

    private Callback callback;
    private String pendingEmail;

    /* ---------------- Constructor ---------------- */

    public UserProfileConsentHelper(AppCompatActivity activity) {
        this.activity = activity;

        // ✅ Registered EARLY and ONCE (lifecycle-safe)
        this.consentLauncher =
                activity.registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {

                            if (callback == null || pendingEmail == null) return;

                            if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                                Log.d(TAG, "Profile consent granted from UI");
                                callback.onConsentGranted(pendingEmail);
                            } else {
                                Log.w(TAG, "Profile consent denied by user");
                                callback.onConsentDenied(pendingEmail);
                            }

                            pendingEmail = null;
                        }
                );
    }

    /* ---------------- Public API ---------------- */

    public void launch(String email, Callback callback) {

        this.pendingEmail = email;
        this.callback = callback;

        Log.d(TAG, "Checking profile consent for " + email);

        executor.execute(() -> {
            try {
                GoogleAccountCredential credential =
                        GoogleAccountCredential.usingOAuth2(
                                activity.getApplicationContext(),
                                Collections.singleton(PROFILE_SCOPE)
                        );
                credential.setSelectedAccountName(email);

                // 🔑 Consent probe
                credential.getToken();

                Log.d(TAG, "Profile consent already granted");
                mainHandler.post(() ->
                        callback.onConsentGranted(email)
                );

            }
            // 🔁 Google Play Services flow
            catch (UserRecoverableAuthException e) {
                Log.w(TAG, "Profile consent required (GMS)");
                mainHandler.post(() ->
                        consentLauncher.launch(e.getIntent())
                );
            }
            // 🔁 HTTP / Drive-style flow
            catch (UserRecoverableAuthIOException e) {
                Log.w(TAG, "Profile consent required (HTTP)");
                mainHandler.post(() ->
                        consentLauncher.launch(e.getIntent())
                );
            }
            catch (Exception e) {
                Log.e(TAG, "Profile consent check failed", e);
                mainHandler.post(() ->
                        callback.onFailure(email, e)
                );
            }
        });
    }
}
