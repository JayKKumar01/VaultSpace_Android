package com.github.jaykkumar01.vaultspace.media.datasource;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.auth.DriveAuthGate;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

final class DriveHttpSource implements DriveSource {

    private static final String TAG = "Drive:HttpSource";
    private static final String BASE_URL =
            "https://www.googleapis.com/drive/v3/files/";

    private final Context context;
    private final String fileId;

    private volatile HttpURLConnection connection;
    private volatile InputStream stream;

    DriveHttpSource(Context context,String fileId) {
        this.context = context.getApplicationContext();
        this.fileId = fileId;
    }

    @Override
    public synchronized InputStream openStream(long position) throws IOException {

        close(); // ensure clean state before opening

        long start = SystemClock.elapsedRealtime();

        String token = DriveAuthGate.get(context).getToken();

        HttpURLConnection conn = getUrlConnection(position, token);

        int code = conn.getResponseCode();
        if (code != 200 && code != 206)
            throw new IOException("HTTP " + code);

        connection = conn;
        stream = conn.getInputStream();

        Log.d(TAG,
                "open @" + position +
                " code=" + code +
                " +" + (SystemClock.elapsedRealtime() - start) + "ms"
        );

        return stream;
    }

    @NonNull
    private HttpURLConnection getUrlConnection(long position, String token) throws IOException {
        URL url = new URL(BASE_URL + fileId + "?alt=media");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization","Bearer " + token);
        conn.setRequestProperty("Accept-Encoding","identity");

        if (position > 0)
            conn.setRequestProperty("Range","bytes=" + position + "-");

        conn.setConnectTimeout(8000);
        conn.setReadTimeout(20000);
        conn.setUseCaches(false);
        conn.setInstanceFollowRedirects(true);

        conn.connect();
        return conn;
    }

    @Override
    public synchronized void close() {

        try { if (stream != null) stream.close(); }
        catch (Exception ignored) {}

        if (connection != null)
            connection.disconnect();

        stream = null;
        connection = null;

        Log.d(TAG,"closed");
    }
}
