package com.github.jaykkumar01.vaultspace.core.auth;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import java.io.IOException;

/**
 * Process-wide auth gate.
 * The ONLY place allowed to call credential.getToken().
 */
public final class DriveAuthGate {

    private static final String TAG = "Video:DriveAuth";

    private static volatile DriveAuthGate INSTANCE;

    private final GoogleAccountCredential credential;
    private volatile String token;

    private DriveAuthGate(Context context) {
        this.credential = GoogleCredentialFactory.forPrimaryDrive(context);
    }

    public static DriveAuthGate get(Context context) {
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
    public synchronized String requireToken() throws IOException {
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
    public synchronized String refreshTokenAfterFailure() throws IOException {
        try {
            Log.w(TAG, "[auth] token refresh after failure");
            token = credential.getToken();
            return token;
        } catch (GoogleAuthException e) {
            throw new IOException(e);
        }
    }
}
