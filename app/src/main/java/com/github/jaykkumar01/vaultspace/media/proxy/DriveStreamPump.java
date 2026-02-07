package com.github.jaykkumar01.vaultspace.media.proxy;

import android.util.Log;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class DriveStreamPump implements Runnable {

    private static final String TAG = "Proxy:DrivePump";

    private final String driveUrl;
    private final String token;
    private final SharedBuffer buffer;

    private Thread thread;
    private volatile boolean stopped;

    public DriveStreamPump(String driveUrl, String token, SharedBuffer buffer) {
        this.driveUrl = driveUrl;
        this.token = token;
        this.buffer = buffer;
        Log.d(TAG, "ctor");
    }

    public void start() {
        stopped = false;
        thread = new Thread(this, "DriveStreamPump");
        thread.start();
        Log.d(TAG, "start()");
    }

    public void stop() {
        stopped = true;
        if (thread != null) thread.interrupt();
        Log.d(TAG, "stop()");
    }

    @Override
    public void run() {
        Log.d(TAG, "run() begin");

        HttpURLConnection conn = null;
        InputStream in = null;

        try {
            conn = (HttpURLConnection) new URL(driveUrl).openConnection();
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Accept-Encoding", "identity");
            conn.connect();

            in = conn.getInputStream();
            byte[] buf = new byte[64 * 1024];
            int n;

            while (!stopped && (n = in.read(buf)) != -1) {
                buffer.write(buf, n);
            }

        } catch (Exception e) {
            Log.e(TAG, "pump error", e);
        } finally {
            buffer.close();
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            if (conn != null) conn.disconnect();
        }

        Log.d(TAG, "run() end");
    }
}
