package com.github.jaykkumar01.vaultspace.media.proxy;

import android.util.Log;

import java.io.*;
import java.net.*;
import java.util.concurrent.CountDownLatch;

public final class ProgressiveProxyController {

    private static final String TAG = "Proxy:ProgressiveProxy";

    private final String driveUrl;
    private final String token;
    private final long fileSize;

    private ServerSocket server;
    private Thread serverThread;
    private volatile boolean running;
    private int port;

    private final CountDownLatch readyLatch = new CountDownLatch(1);

    /* ======================= Constructor ======================= */

    public ProgressiveProxyController(String driveUrl, String token, long fileSize) {
        Log.d(TAG, "ctor()");
        Log.d(TAG, "Drive URL = " + driveUrl);
        Log.d(TAG, "File size = " + fileSize);

        this.driveUrl = driveUrl;
        this.token = token;
        this.fileSize = fileSize;
    }

    /* ======================= Public API ======================= */

    public void start() {
        Log.d(TAG, "start()");
        running = true;
        serverThread = new Thread(this::runServer, "MP4-Proxy");
        serverThread.start();

        try {
            readyLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Proxy start interrupted", e);
        }

        Log.d(TAG, "Proxy ready on port " + port);
    }

    public void stop() {
        Log.d(TAG, "stop()");
        running = false;
        try {
            if (server != null) server.close();
        } catch (IOException ignored) {}
    }

    public String videoUrl() {
        return "http://127.0.0.1:" + port + "/video.mp4";
    }

    /* ======================= Server Loop ======================= */

    private void runServer() {
        try {
            server = new ServerSocket(0);
            port = server.getLocalPort();
            Log.d(TAG, "Bound on port " + port);
            readyLatch.countDown();

            while (running) {
                Socket client = server.accept();
                new Thread(() -> handleClient(client), "MP4-Client").start();
            }
        } catch (IOException e) {
            Log.d(TAG, "Server loop ended");
        }
    }

    /* ======================= Request Handling ======================= */

    private void handleClient(Socket client) {
        try (
                BufferedReader in =
                        new BufferedReader(new InputStreamReader(client.getInputStream()));
                OutputStream out = client.getOutputStream()
        ) {
            String requestLine = in.readLine();
            if (requestLine == null) return;

            Log.d(TAG, "Request: " + requestLine);

            long start = 0;
            long end = fileSize - 1;

            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Range:")) {
                    long[] r = parseRange(line);
                    start = r[0];
                    end = r[1];
                }
            }

            if (start < 0 || start >= fileSize) {
                Log.w(TAG, "Invalid range start=" + start);
                return;
            }

            if (end >= fileSize) end = fileSize - 1;

            Log.d(TAG, "Serving range " + start + "-" + end);
            streamRange(start, end, out);

        } catch (Exception e) {
            Log.d(TAG, "Client disconnected");
        }
    }

    /* ======================= Streaming ======================= */

    private void streamRange(long start, long end, OutputStream out) throws IOException {
        HttpURLConnection conn =
                (HttpURLConnection) new URL(driveUrl).openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Range", "bytes=" + start + "-" + end);
        conn.connect();

        long length = end - start + 1;

        out.write((
                "HTTP/1.1 206 Partial Content\r\n" +
                "Content-Type: video/mp4\r\n" +
                "Content-Length: " + length + "\r\n" +
                "Content-Range: bytes " + start + "-" + end + "/" + fileSize + "\r\n" +
                "Accept-Ranges: bytes\r\n\r\n"
        ).getBytes());

        try (InputStream in = conn.getInputStream()) {
            byte[] buf = new byte[64 * 1024];
            int r;
            while ((r = in.read(buf)) != -1) {
                out.write(buf, 0, r);
                out.flush();
            }
        } catch (IOException e) {
            Log.d(TAG, "Stream cancelled by client");
        } finally {
            conn.disconnect();
        }
    }

    /* ======================= Helpers ======================= */

    private long[] parseRange(String header) {
        // Range: bytes=START-END
        int eq = header.indexOf('=');
        int dash = header.indexOf('-', eq);
        long start = Long.parseLong(header.substring(eq + 1, dash).trim());
        String endStr = header.substring(dash + 1).trim();
        long end = endStr.isEmpty() ? fileSize - 1 : Long.parseLong(endStr);
        return new long[]{start, end};
    }
}
