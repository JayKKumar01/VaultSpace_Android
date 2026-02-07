package com.github.jaykkumar01.vaultspace.media.proxy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public final class LocalHttpServer implements Runnable {

    public interface RangeListener {
        void onRangeRequested(long offset);

        void onClientDisconnected();
    }

    private final int port;
    private final SharedBuffer buffer;
    private final RangeListener listener;

    private Thread thread;
    private volatile boolean stopped;

    public LocalHttpServer(int port,
                           SharedBuffer buffer,
                           RangeListener listener) {
        this.port = port;
        this.buffer = buffer;
        this.listener = listener;
    }

    public void start() {
        stopped = false;
        thread = new Thread(this, "LocalHttpServer");
        thread.start();
    }

    public void stop() {
        stopped = true;
        if (thread != null) thread.interrupt();
    }

    @Override
    public void run() {
        try (ServerSocket server = new ServerSocket(port)) {
            while (!stopped) {
                try (Socket client = server.accept()) {
                    handleClient(client);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void handleClient(Socket client) {
        try {
            InputStream in = new BufferedInputStream(client.getInputStream());
            OutputStream out = new BufferedOutputStream(client.getOutputStream());

            long rangeStart = parseRange(in);
            if (rangeStart > 0)
                listener.onRangeRequested(rangeStart);

            writeHeaders(out, rangeStart);

            byte[] chunk = new byte[32 * 1024];
            int read;

            while ((read = buffer.read(chunk)) != -1) {
                out.write(chunk, 0, read);
                out.flush();
            }

        } catch (Exception ignored) {
        } finally {
            listener.onClientDisconnected();
        }
    }

    private long parseRange(InputStream in) throws Exception {
        StringBuilder line = new StringBuilder();
        long range = 0;

        while (true) {
            int c = in.read();
            if (c == -1) break;
            if (c == '\n') {
                String l = line.toString();
                if (l.startsWith("Range:")) {
                    int idx = l.indexOf("bytes=");
                    if (idx != -1) {
                        String v = l.substring(idx + 6).replace("-", "").trim();
                        range = Long.parseLong(v);
                    }
                }
                if (l.isEmpty()) break;
                line.setLength(0);
            } else if (c != '\r') {
                line.append((char) c);
            }
        }
        return range;
    }

    private void writeHeaders(OutputStream out, long offset) throws Exception {
        String headers = "HTTP/1.1 206 Partial Content\r\n" + "Content-Type: application/octet-stream\r\n" + "Accept-Ranges: bytes\r\n" + "Content-Range: bytes " + offset + "-/*\r\n" + "Connection: close\r\n" + "\r\n";
        out.write(headers.getBytes());
        out.flush();
    }
}
