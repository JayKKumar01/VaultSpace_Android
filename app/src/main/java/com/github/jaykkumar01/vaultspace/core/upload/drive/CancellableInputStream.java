package com.github.jaykkumar01.vaultspace.core.upload.drive;

import com.github.jaykkumar01.vaultspace.core.upload.helper.CancelToken;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CancellationException;

final class CancellableInputStream extends InputStream {

    private final InputStream in;
    private final CancelToken token;
    private volatile boolean closed;

    CancellableInputStream(InputStream in, CancelToken token) {
        this.in = in;
        this.token = token;
    }

    private void check() throws CancellationException {
        if (token.isCancelled()) {
            closeQuietly();
            throw new CancellationException();
        }
    }

    @Override
    public int read() throws IOException {
        check();
        return in.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        check();
        return in.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;
        in.close();
    }

    private void closeQuietly() {
        try { close(); } catch (IOException ignored) {}
    }
}
