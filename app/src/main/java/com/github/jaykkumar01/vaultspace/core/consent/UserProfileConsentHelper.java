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

    public interface Callback {
        void onConsentGranted();
        void onConsentDenied();
        void onFailure(Exception e);
    }

    private final AppCompatActivity activity;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ActivityResultLauncher<Intent> consentLauncher;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Callback callback;

    public UserProfileConsentHelper(
            AppCompatActivity activity,
            Callback callback
    ) {
        this.activity = activity;
        this.callback = callback;

        this.consentLauncher =
                activity.registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                                Log.d(TAG, "Profile consent granted from UI");
                                callback.onConsentGranted();
                            } else {
                                Log.w(TAG, "Profile consent denied by user");
                                callback.onConsentDenied();
                            }
                        }
                );
    }

    public void launch(String email) {
        Log.d(TAG, "launch() → checking profile consent for " + email);

        executor.execute(() -> {
            try {
                GoogleAccountCredential credential =
                        GoogleAccountCredential.usingOAuth2(
                                activity.getApplicationContext(),
                                Collections.singleton(
                                        "https://www.googleapis.com/auth/userinfo.profile"
                                )
                        );
                credential.setSelectedAccountName(email);

                // 🔑 Consent probe
                credential.getToken();

                Log.d(TAG, "Profile consent already granted");
                mainHandler.post(callback::onConsentGranted);

            }
            // ✅ HANDLE THIS (your missing piece)
            catch (UserRecoverableAuthException e) {
                Log.w(TAG, "Profile consent required (GMS)");
                mainHandler.post(() ->
                        consentLauncher.launch(e.getIntent())
                );
            }
            // ✅ ALSO KEEP THIS (future-proof)
            catch (UserRecoverableAuthIOException e) {
                Log.w(TAG, "Profile consent required (HTTP)");
                mainHandler.post(() ->
                        consentLauncher.launch(e.getIntent())
                );
            }
            catch (Exception e) {
                Log.e(TAG, "Profile consent check failed", e);
                mainHandler.post(() ->
                        callback.onFailure(e)
                );
            }
        });
    }
}
