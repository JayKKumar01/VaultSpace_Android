package com.github.jaykkumar01.vaultspace.media.proxy;

import android.util.Log;

import java.io.*;
import java.net.*;
import java.util.concurrent.CountDownLatch;

public final class ProxyController {

    private static final String TAG = "Proxy:Controller";
    private static final int HEAD_SIZE = 1024 * 1024 * 2; // 2 MB

    private final String driveUrl;
    private final String token;

    private ServerSocket server;
    private volatile boolean running;

    private int port;
    private byte[] headBuffer;
    private int headLength;

    private final CountDownLatch readyLatch = new CountDownLatch(1);

    public ProxyController(String driveUrl, String token) {
        this.driveUrl = driveUrl;
        this.token = token;
    }

    /* ======================= Public API ======================= */

    public void start() {
        running = true;
        Thread serverThread = new Thread(this::run, "DriveProxy");
        serverThread.start();
        awaitReady();
    }

    public void stop() {
        running = false;
        try { if (server != null) server.close(); } catch (Exception ignored) {}
    }

    public String getProxyUrl() {
        return "http://127.0.0.1:" + port + "/video";
    }

    /* ======================= Core ======================= */

    private void run() {
        try {
            prefetchHead();

            server = new ServerSocket(0);
            port = server.getLocalPort();

            Log.d(TAG, "Bound on port = " + port);
            readyLatch.countDown();

            while (running) {
                Socket client = server.accept();
                handleClient(client);
            }
        } catch (IOException e) {
            Log.d(TAG, "Proxy stopped");
        }
    }

    private void awaitReady() {
        try { readyLatch.await(); } catch (InterruptedException ignored) {}
    }

    /* ======================= Prefetch ======================= */

    private void prefetchHead() throws IOException {
        HttpURLConnection conn = openDriveConnection(0);
        InputStream in = conn.getInputStream();

        headBuffer = new byte[HEAD_SIZE];
        headLength = 0;

        while (headLength < HEAD_SIZE) {
            int r = in.read(headBuffer, headLength, HEAD_SIZE - headLength);
            if (r == -1) break;
            headLength += r;
        }

        in.close();
        Log.d(TAG, "Head prefetched = " + headLength + " bytes");
    }

    /* ======================= Client ======================= */

    private void handleClient(Socket client) {
        try (
                InputStream in = client.getInputStream();
                OutputStream out = client.getOutputStream()
        ) {
            long range = parseRange(in);

            HttpURLConnection conn = openDriveConnection(range);
            long contentLength = conn.getContentLengthLong();
            long end = range + contentLength - 1;

            writeHeaders(out, range, end, contentLength);

            if (range < headLength) {
                int len = (int) Math.min(headLength - range, contentLength);
                out.write(headBuffer, (int) range, len);
                out.flush();
                if (len == contentLength) return;
                range += len;
            }

            streamFromDrive(conn.getInputStream(), out);

        } catch (SocketException | EOFException ignored) {
            // client closed connection intentionally
        } catch (Exception e) {
            Log.e(TAG, "Unexpected proxy error", e);
        }
    }

    /* ======================= Helpers ======================= */

    private long parseRange(InputStream in) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(in));
        String line;
        long range = 0;
        while ((line = r.readLine()) != null && !line.isEmpty()) {
            if (line.startsWith("Range:")) {
                range = Long.parseLong(line.replaceAll("\\D+", ""));
            }
        }
        return range;
    }

    private HttpURLConnection openDriveConnection(long range) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(driveUrl).openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Range", "bytes=" + range + "-");
        conn.connect();
        return conn;
    }

    private void writeHeaders(OutputStream out, long start, long end, long len) throws IOException {
        String h =
                "HTTP/1.1 206 Partial Content\r\n" +
                        "Content-Type: video/mp4\r\n" +
                        "Accept-Ranges: bytes\r\n" +
                        "Content-Range: bytes " + start + "-" + end + "/*\r\n" +
                        "Content-Length: " + len + "\r\n\r\n";
        out.write(h.getBytes());
        out.flush();
    }

    private void streamFromDrive(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[64 * 1024];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
            out.flush();
        }
        in.close();
    }
}
