package com.github.jaykkumar01.vaultspace.core.auth;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Process-wide auth gate for Google Drive access.
 * File-independent, ultra-light validation.
 */
public final class DriveAuthGate {
    private static volatile DriveAuthGate INSTANCE;

    private final GoogleAccountCredential credential;

    /* ---------------- lifecycle ---------------- */

    private DriveAuthGate(Context context) {
        this.credential = GoogleCredentialFactory.forPrimaryDrive(context);
    }

    public static DriveAuthGate get(Context context) {
        if (INSTANCE == null) {
            synchronized (DriveAuthGate.class) {
                if (INSTANCE == null)
                    INSTANCE = new DriveAuthGate(
                            context.getApplicationContext());
            }
        }
        return INSTANCE;
    }

    /* ---------------- public api ---------------- */

    public String getToken() {
        try {
            return credential.getToken();
        } catch (IOException | GoogleAuthException e) {
            throw new RuntimeException(e);
        }
    }

}
