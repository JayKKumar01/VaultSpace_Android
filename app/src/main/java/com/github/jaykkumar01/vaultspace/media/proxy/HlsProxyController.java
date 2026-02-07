package com.github.jaykkumar01.vaultspace.media.proxy;

import android.util.Log;

import java.io.*;
import java.net.*;
import java.util.concurrent.CountDownLatch;

public final class HlsProxyController {

    private static final String TAG = "Proxy:HlsProxyController";

    private final String driveUrl;
    private final String token;
    private final HlsSegmenter segmenter;

    private ServerSocket server;
    private Thread serverThread;
    private volatile boolean running;
    private int port;

    private final CountDownLatch readyLatch = new CountDownLatch(1);

    public HlsProxyController(String driveUrl, String token, long fileSize) {
        Log.d(TAG, "ctor()");
        Log.d(TAG, "Drive URL = " + driveUrl);
        Log.d(TAG, "File size = " + fileSize);

        this.driveUrl = driveUrl;
        this.token = token;
        this.segmenter = new HlsSegmenter(fileSize);
    }

    /* ======================= Public API ======================= */

    public void start() {
        Log.d(TAG, "start()");
        running = true;
        serverThread = new Thread(this::runServer, "HLS-Proxy");
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

    public String playlistUrl() {
        return "http://127.0.0.1:" + port + "/playlist.m3u8";
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
                new Thread(() -> handleClient(client), "HLS-Client").start();
            }
        } catch (IOException e) {
            Log.d(TAG, "Server loop ended");
        }
    }

    /* ======================= Request Handling ======================= */

    private void handleClient(Socket client) {
        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(client.getInputStream()));
                OutputStream out = client.getOutputStream()
        ) {
            String requestLine = in.readLine();
            if (requestLine == null) return;

            Log.d(TAG, "Request: " + requestLine);

            if (requestLine.contains("playlist.m3u8")) {
                sendPlaylist(out);
                return;
            }

            if (requestLine.contains("/seg_") && requestLine.contains(".m4s")) {
                int index = parseSegmentIndex(requestLine);

                if (index < 0 || index >= segmenter.segmentCount()) {
                    Log.w(TAG, "Invalid segment index " + index);
                    return;
                }

                sendSegment(index, out);
                return;
            }

            Log.w(TAG, "Unknown request");

        } catch (Exception e) {
            Log.d(TAG, "Client disconnected");
        }
    }

    /* ======================= Responses ======================= */

    private void sendPlaylist(OutputStream out) throws IOException {
        byte[] body = segmenter.buildPlaylist().getBytes();
        writeHeaders(out, "application/vnd.apple.mpegurl", body.length);
        out.write(body);
        out.flush();
    }

    private void sendSegment(int index, OutputStream out) throws IOException {
        long start = segmenter.start(index);
        long end = segmenter.end(index);
        long length = end - start + 1;

        Log.d(TAG, "Segment " + index + " range " + start + "-" + end);

        HttpURLConnection conn =
                (HttpURLConnection) new URL(driveUrl).openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Range", "bytes=" + start + "-" + end);
        conn.connect();

        writeHeaders(out, "video/mp4", length);

        try (InputStream in = conn.getInputStream()) {
            byte[] buf = new byte[64 * 1024];
            int r;
            while ((r = in.read(buf)) != -1) {
                out.write(buf, 0, r);
                out.flush();
            }
        } catch (IOException e) {
            Log.d(TAG, "Segment " + index + " cancelled");
        } finally {
            conn.disconnect();
        }
    }

    /* ======================= Helpers ======================= */

    private int parseSegmentIndex(String requestLine) {
        int s = requestLine.indexOf("/seg_");
        int e = requestLine.indexOf(".m4s");
        return Integer.parseInt(requestLine.substring(s + 5, e));
    }

    private void writeHeaders(OutputStream out, String type, long len)
            throws IOException {

        out.write((
                "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + type + "\r\n" +
                "Content-Length: " + len + "\r\n\r\n"
        ).getBytes());
    }
}
