package com.github.jaykkumar01.vaultspace.media.datasource;

import android.content.Context;
import android.util.Log;

import com.github.jaykkumar01.vaultspace.core.auth.GoogleCredentialFactory;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import java.io.IOException;

/**
 * Process-wide auth gate.
 * The ONLY place allowed to call credential.getToken().
 */
final class DriveAuthGate {

    private static final String TAG = "Video:DriveAuth";

    private static volatile DriveAuthGate INSTANCE;

    private final GoogleAccountCredential credential;
    private volatile String token;

    private DriveAuthGate(Context context) {
        this.credential = GoogleCredentialFactory.forPrimaryDrive(context);
    }

    static DriveAuthGate get(Context context) {
        if (INSTANCE == null) {
            synchronized (DriveAuthGate.class) {
                if (INSTANCE == null) {
                    INSTANCE = new DriveAuthGate(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Returns cached token.
     * Fetches ONLY once if token is null.
     */
    synchronized String requireToken() throws IOException {
        if (token != null) return token;

        try {
            Log.d(TAG, "[auth] initial token fetch");
            token = credential.getToken();
            return token;
        } catch (GoogleAuthException e) {
            throw new IOException(e);
        }
    }

    /**
     * Fetches a new token ONLY after a real HTTP failure.
     */
    synchronized String refreshTokenAfterFailure() throws IOException {
        try {
            Log.w(TAG, "[auth] token refresh after failure");
            token = credential.getToken();
            return token;
        } catch (GoogleAuthException e) {
            throw new IOException(e);
        }
    }
}
