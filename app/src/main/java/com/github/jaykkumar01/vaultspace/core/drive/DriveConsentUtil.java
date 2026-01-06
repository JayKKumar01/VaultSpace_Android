package com.github.jaykkumar01.vaultspace.core.drive;

import android.util.Log;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.drive.Drive;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

public class DriveConsentUtil {

    private static final String TAG = "VaultSpace:Consent";

    public interface ConsentCallback {
        void onGranted();
        void onRecoverable(UserRecoverableAuthIOException e);
        void onFailure(Exception e);
    }

    public static void checkConsent(
            GoogleAccountCredential credential,
            ConsentCallback callback
    ) {
        new Thread(() -> {
            try {
                Log.d(TAG, "Checking Drive consent");

                Drive drive =
                        new Drive.Builder(
                                new NetHttpTransport(),
                                GsonFactory.getDefaultInstance(),
                                credential
                        )
                                .setApplicationName("VaultSpace")
                                .build();

                // Lightweight auth trigger
                drive.about().get().setFields("user").execute();

                Log.d(TAG, "Drive consent valid");
                callback.onGranted();

            } catch (UserRecoverableAuthIOException e) {
                Log.w(TAG, "Drive consent revoked / required");
                callback.onRecoverable(e);

            } catch (Exception e) {
                Log.e(TAG, "Consent check failed", e);
                callback.onFailure(e);
            }
        }).start();
    }
}
