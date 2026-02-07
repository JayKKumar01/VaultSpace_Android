package com.github.jaykkumar01.vaultspace.media.proxy;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class DriveStreamPump implements Runnable {

    private final String driveUrl;
    private final String token;
    private final long startOffset;
    private final SharedBuffer buffer;

    private Thread thread;
    private volatile boolean stopped;

    public DriveStreamPump(String driveUrl,
                           String oauthToken,
                           long startOffset,
                           SharedBuffer buffer) {
        this.driveUrl = driveUrl;
        this.token = oauthToken;
        this.startOffset = startOffset;
        this.buffer = buffer;
    }

    public void start() {
        stopped = false;
        thread = new Thread(this, "DriveStreamPump");
        thread.start();
    }

    public void stop() {
        stopped = true;
        if (thread != null) thread.interrupt();
    }

    @Override
    public void run() {
        HttpURLConnection conn = null;
        InputStream in = null;

        try {
            conn = (HttpURLConnection) new URL(driveUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(15_000);

            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Accept-Encoding", "identity");
            conn.setRequestProperty("Range", "bytes=" + startOffset + "-");

            conn.connect();

            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK &&
                    code != HttpURLConnection.HTTP_PARTIAL) {
                throw new IllegalStateException("Drive HTTP " + code);
            }

            in = conn.getInputStream();

            byte[] chunk = new byte[64 * 1024];
            int read;

            while (!stopped && (read = in.read(chunk)) != -1) {
                buffer.write(chunk, read);
            }

        } catch (Exception e) {
            buffer.close();
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            if (conn != null) conn.disconnect();
        }
    }
}
