package com.github.jaykkumar01.vaultspace.media.datasource;

import android.content.Context;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.auth.DriveAuthGate;
import com.github.jaykkumar01.vaultspace.media.base.DriveStreamSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

final class DriveHttpSource implements DriveStreamSource {

    private static final String BASE_URL = "https://www.googleapis.com/drive/v3/files/";
    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 20000;

    private final String fileId;
    private final Context appContext;

    // Token cached per instance
    private String token;

    DriveHttpSource(Context context, String fileId) {
        this.appContext = context.getApplicationContext();
        this.fileId = fileId;
    }

    /* ========================= OPEN ========================= */

    @Override
    public StreamSession open(long position) throws IOException {

        HttpURLConnection conn = buildConnection(position, false);

        int code = conn.getResponseCode();

        // If token expired, retry once with fresh token
        if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
            conn.disconnect();
            token = null;
            conn = buildConnection(position, true);
            code = conn.getResponseCode();
        }

        if (code != HttpURLConnection.HTTP_OK &&
                code != HttpURLConnection.HTTP_PARTIAL)
            throw new IOException("HTTP " + code);

        long length = conn.getContentLengthLong();
        InputStream stream = conn.getInputStream();

        HttpURLConnection finalConn = conn;
        return new StreamSession() {

            @Override
            public InputStream stream() {
                return stream;
            }

            @Override
            public long length() {
                return length;
            }

            @Override
            public void cancel() {
                try { stream.close(); } catch (Exception ignored) {}
                // Do NOT aggressively disconnect if fully read
                finalConn.disconnect();
            }
        };
    }

    /* ========================= CONNECTION ========================= */

    @NonNull
    private HttpURLConnection buildConnection(long position, boolean forceRefreshToken) throws IOException {

        if (token == null || forceRefreshToken) {
            token = DriveAuthGate.get(appContext).getToken();
        }

        URL url = new URL(BASE_URL + fileId + "?alt=media");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);

        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Accept-Encoding", "identity");
        conn.setRequestProperty("Connection", "keep-alive");

        if (position > 0) {
            conn.setRequestProperty("Range", "bytes=" + position + "-");
        }

        conn.setUseCaches(false);

        return conn;
    }
}
