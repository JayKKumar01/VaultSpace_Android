package com.github.jaykkumar01.vaultspace.core.auth;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Process-wide auth gate for Drive alt=media access.
 * The ONLY place allowed to call credential.getToken().
 */
public final class DriveAltMediaAuthGate {

    private static final String TAG = "Video:DriveAltMediaAuth";
    private static final String BASE = "https://www.googleapis.com/drive/v3/files/";
    private static volatile DriveAltMediaAuthGate INSTANCE;

    private final GoogleAccountCredential credential;
    private volatile String token;

    /* ---------------- lifecycle ---------------- */

    private DriveAltMediaAuthGate(Context context) {
        this.credential = GoogleCredentialFactory.forPrimaryDrive(context);
    }

    public static DriveAltMediaAuthGate get(Context context) {
        if (INSTANCE == null) {
            synchronized (DriveAltMediaAuthGate.class) {
                if (INSTANCE == null)
                    INSTANCE = new DriveAltMediaAuthGate(context.getApplicationContext());
            }
        }
        return INSTANCE;
    }

    /* ---------------- public api ---------------- */

    /**
     * Returns a valid token for Drive alt=media.
     * - Fetch once if null
     * - Validate via alt=media probe
     * - Refresh only on 401 / 403
     */
    public synchronized String requireValidToken(@NonNull String fileId) {
        if (token == null) {
            Log.d(TAG, "[auth] initial token fetch");
            token = fetchToken();
            return token;
        }

        if (probeAltMedia(fileId, token))
            return token;

        Log.w(TAG, "[auth] token invalid, refreshing");
        token = fetchToken();
        return token;
    }

    /* ---------------- internals ---------------- */

    private String fetchToken() {
        try {
            return credential.getToken();
        } catch (IOException | GoogleAuthException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean probeAltMedia(@NonNull String fileId,
                                  @NonNull String token) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(BASE + fileId + "?alt=media");

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Range", "bytes=0-0");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            int code = conn.getResponseCode();
            return code != 401 && code != 403;

        } catch (IOException e) {
            // network failure ≠ invalid token
            return true;

        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
