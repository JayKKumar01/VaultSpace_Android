package com.github.jaykkumar01.vaultspace.media.proxy;

import java.util.concurrent.atomic.AtomicBoolean;

public final class ProxyController implements LocalHttpServer.RangeListener {

    private static final int PORT = 8877;
    private static final int BUFFER_BYTES = 8 * 1024 * 1024;

    private final String driveUrl;
    private final String token;

    private SharedBuffer buffer;
    private DriveStreamPump pump;
    private LocalHttpServer server;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public ProxyController(String driveUrl, String token) {
        this.driveUrl = driveUrl;
        this.token = token;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) return;

        buffer = new SharedBuffer(BUFFER_BYTES);
        server = new LocalHttpServer(PORT, buffer, this);
        pump = new DriveStreamPump(driveUrl, token, 0, buffer);

        server.start();
        pump.start();
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) return;

        if (pump != null) pump.stop();
        if (server != null) server.stop();
        if (buffer != null) buffer.close();

        pump = null;
        server = null;
        buffer = null;
    }

    @Override
    public synchronized void onRangeRequested(long offset) {
        if (!running.get()) return;

        if (pump != null) pump.stop();
        if (buffer != null) buffer.clear();

        pump = new DriveStreamPump(driveUrl, token, offset, buffer);
        pump.start();
    }

    @Override
    public void onClientDisconnected() {
        stop();
    }

    public String getProxyUrl() {
        return "http://127.0.0.1:" + PORT + "/stream";
    }
}
