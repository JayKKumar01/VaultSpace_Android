package com.github.jaykkumar01.vaultspace.media.proxy;

import android.os.Handler;
import android.os.Looper;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class FastStartProxyController {

    public interface Callback {
        void onReady(String localUrl);
    }

    private final String driveUrl;
    private final String token;
    private final long fileSize;

    private ServerSocket server;
    private volatile boolean running;

    private final ExecutorService clientExecutor =
            Executors.newCachedThreadPool();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private Callback callback;

    public FastStartProxyController(String driveUrl,
                                    String token,
                                    long fileSize) {
        this.driveUrl = driveUrl;
        this.token = token;
        this.fileSize = fileSize;
    }

    /* ======================= Public API ======================= */

    public void start(Callback cb) {
        this.callback = cb;
        running = true;
        new Thread(this::runServer, "Proxy-Server").start();
    }

    public void stop() {
        running = false;
        clientExecutor.shutdownNow();
        try { if (server != null) server.close(); } catch (IOException ignored) {}
        callback = null;
    }

    /* ======================= Server ======================= */

    private void runServer() {
        try {
            server = new ServerSocket(0);
            int port = server.getLocalPort();

            // ✅ SAFE callback dispatch
            final Callback cb = callback;
            callback = null;

            if (cb != null) {
                String url = "http://127.0.0.1:" + port + "/video.mp4";
                mainHandler.post(() -> cb.onReady(url));
            }

            while (running) {
                Socket client = server.accept();
                clientExecutor.execute(() -> handleClient(client));
            }
        } catch (IOException ignored) {}
    }

    /* ======================= Client ======================= */

    private void handleClient(Socket client) {
        try (
                BufferedReader in =
                        new BufferedReader(new InputStreamReader(client.getInputStream()));
                OutputStream out = client.getOutputStream()
        ) {
            String request = in.readLine();
            if (request == null) return;

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

            if (end >= fileSize) end = fileSize - 1;
            long length = end - start + 1;

            out.write((
                    "HTTP/1.1 206 Partial Content\r\n" +
                            "Content-Type: video/mp4\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Content-Range: bytes " + start + "-" + end + "/" + fileSize + "\r\n" +
                            "Accept-Ranges: bytes\r\n\r\n"
            ).getBytes());

            streamFromDrive(start, end, out);

        } catch (Exception ignored) {}
    }

    /* ======================= Drive ======================= */

    private void streamFromDrive(long start, long end, OutputStream out)
            throws IOException {

        HttpURLConnection conn =
                (HttpURLConnection) new URL(driveUrl).openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Range", "bytes=" + start + "-" + end);
        conn.connect();

        try (InputStream in = conn.getInputStream()) {
            byte[] buf = new byte[64 * 1024];
            int r;
            while ((r = in.read(buf)) != -1) {
                out.write(buf, 0, r);
                out.flush();
            }
        } catch (IOException ignored) {
            // ExoPlayer cancelled – expected
        } finally {
            conn.disconnect();
        }
    }

    /* ======================= Helpers ======================= */

    private long[] parseRange(String h) {
        int eq = h.indexOf('=');
        int dash = h.indexOf('-', eq);
        long s = Long.parseLong(h.substring(eq + 1, dash).trim());
        String e = h.substring(dash + 1).trim();
        long end = e.isEmpty() ? fileSize - 1 : Long.parseLong(e);
        return new long[]{s, end};
    }
}
