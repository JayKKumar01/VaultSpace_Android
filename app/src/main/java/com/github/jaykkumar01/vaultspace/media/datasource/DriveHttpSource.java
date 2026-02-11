package com.github.jaykkumar01.vaultspace.media.datasource;

import android.content.Context;
import com.github.jaykkumar01.vaultspace.core.auth.DriveAuthGate;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

final class DriveHttpSource implements DriveStreamSource {

    private static final String BASE_URL = "https://www.googleapis.com/drive/v3/files/";

    private final Context context;
    private final String fileId;
    private String token;

    DriveHttpSource(Context context,String fileId) {
        this.context = context.getApplicationContext();
        this.fileId = fileId;
    }

    @Override
    public StreamSession open(long position) throws IOException {

        if (token == null)
            token = DriveAuthGate.get(context).getToken();

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
        conn.connect();

        int code = conn.getResponseCode();
        if (code != 200 && code != 206)
            throw new IOException("HTTP " + code);

        InputStream stream = conn.getInputStream();

        return new StreamSession() {
            @Override public InputStream stream() { return stream; }
            @Override public void cancel() {
                try { stream.close(); } catch (Exception ignored) {}
                conn.disconnect();
            }
        };
    }
}
