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
public final class DriveAuthGate1 {

    private static final String TAG = "Video:DriveAuth";

    private static volatile DriveAuthGate1 INSTANCE;

    private final GoogleAccountCredential credential;
    private volatile String token;

    private DriveAuthGate1(Context context) {
        this.credential = GoogleCredentialFactory.forPrimaryDrive(context);
    }

    public static DriveAuthGate1 get(Context context) {
        if (INSTANCE == null) {
            synchronized (DriveAuthGate1.class) {
                if (INSTANCE == null) {
                    INSTANCE = new DriveAuthGate1(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Returns cached token.
     * Fetches ONLY once if token is null.
     */
    public synchronized String requireToken(){
        if (token != null) return token;

        Log.d(TAG, "[auth] initial token fetch");
        try {
            token = credential.getToken();
        } catch (IOException | GoogleAuthException e) {
            throw new RuntimeException(e);
        }
        return token;
    }

    /**
     * Fetches a new token ONLY after a real HTTP failure.
     */
    public synchronized String refreshTokenAfterFailure(){
        Log.w(TAG, "[auth] token refresh after failure");
        try {
            token = credential.getToken();
        } catch (IOException | GoogleAuthException e) {
            throw new RuntimeException(e);
        }
        return token;
    }
}
