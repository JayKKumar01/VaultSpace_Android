package com.github.jaykkumar01.vaultspace.core.auth;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.drive.Drive;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import java.util.Arrays;
import java.util.Collections;

public class DriveConsentFlowHelper {

    private static final String TAG = "VaultSpace:DriveConsent";

    public interface Callback {
        void onConsentGranted();
        void onConsentRequired(Intent intent);
        void onFailure(Exception e);
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /* ---------------------------------------------------
     * Public API
     * --------------------------------------------------- */

    public void checkConsent(GoogleAccountCredential credential, Callback callback) {
        Log.d(TAG, "checkConsent() started");

        new Thread(() -> {
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

                Log.d(TAG, "Triggering lightweight Drive auth call");
                drive.about().get().setFields("user").execute();

                Log.d(TAG, "Drive consent VALID");

                mainHandler.post(callback::onConsentGranted);

            } catch (UserRecoverableAuthIOException e) {
                Log.w(TAG, "Drive consent REQUIRED / revoked");

                mainHandler.post(() ->
                        callback.onConsentRequired(e.getIntent())
                );

            } catch (Exception e) {
                Log.e(TAG, "Drive consent check FAILED", e);

                mainHandler.post(() ->
                        callback.onFailure(e)
                );
            }
        }).start();
    }

    /* ---------------------------------------------------
     * Credential Factory
     * --------------------------------------------------- */

    public static GoogleAccountCredential createCredential(Context context) {

        return GoogleAccountCredential.usingOAuth2(
                context,
                Arrays.asList(
                        "https://www.googleapis.com/auth/drive.file",
                        "https://www.googleapis.com/auth/userinfo.profile"
                )
        );
    }

}
