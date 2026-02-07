package com.github.jaykkumar01.vaultspace.media.proxy;

import android.util.Log;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public final class LocalHttpServer implements Runnable {

    private static final String TAG = "Proxy:HttpServer";

    private final int port;
    private final SharedBuffer buffer;

    private volatile boolean stopped;
    private Thread thread;

    public LocalHttpServer(int port, SharedBuffer buffer) {
        this.port = port;
        this.buffer = buffer;
    }

    public void start() {
        thread = new Thread(this, "LocalHttpServer");
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
        try (ServerSocket server = new ServerSocket()) {
            server.setReuseAddress(true);
            server.bind(new InetSocketAddress("127.0.0.1", port));
            Log.d(TAG, "bound 127.0.0.1:" + port);

            while (!stopped) {
                Socket client = server.accept();
                Log.d(TAG, "client connected");
                new Thread(() -> handleClient(client), "HttpClient").start();
            }

        } catch (Exception e) {
            if (!stopped) Log.e(TAG, "server error", e);
        }
    }

    private void handleClient(Socket client) {
        try (
                BufferedInputStream in = new BufferedInputStream(client.getInputStream());
                BufferedOutputStream out = new BufferedOutputStream(client.getOutputStream())
        ) {
            // 🔑 READ REQUEST (mandatory)
            readRequest(in);

            // 🔑 VALID RESPONSE
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: video/mp4\r\n" +
                            "Accept-Ranges: bytes\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();

            byte[] chunk = new byte[32 * 1024];
            int n;

            while (!stopped && (n = buffer.read(chunk)) != -1) {
                out.write(chunk, 0, n);
            }
            out.flush();

        } catch (Exception e) {
            Log.d(TAG, "client disconnected (normal)");
        } finally {
            try { client.close(); } catch (Exception ignored) {}
        }
    }

    private void readRequest(InputStream in) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(in));
        String line;
        while ((line = r.readLine()) != null && !line.isEmpty()) {
            // ignore headers
        }
    }
}
