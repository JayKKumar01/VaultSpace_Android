package com.github.jaykkumar01.vaultspace.media.proxy;

import android.util.Log;

public final class SharedBuffer {

    private static final String TAG = "Proxy:SharedBuffer";

    private final byte[] buffer;
    private int writePos, readPos, size;
    private boolean closed;

    public SharedBuffer(int capacity) {
        buffer = new byte[capacity];
        Log.d(TAG, "init capacity=" + capacity);
    }

    public synchronized void write(byte[] src, int len) throws InterruptedException {
        if (closed) return;

        int off = 0;
        while (off < len) {
            while (size == buffer.length && !closed) wait();
            if (closed) return;

            int space = buffer.length - size;
            int n = Math.min(space, len - off);

            int first = Math.min(n, buffer.length - writePos);
            System.arraycopy(src, off, buffer, writePos, first);
            writePos = (writePos + first) % buffer.length;
            size += first;
            off += first;

            int remain = n - first;
            if (remain > 0) {
                System.arraycopy(src, off, buffer, writePos, remain);
                writePos += remain;
                size += remain;
                off += remain;
            }

            notifyAll();
        }
    }

    public synchronized int read(byte[] out) throws InterruptedException {
        while (size == 0 && !closed) wait();
        if (size == 0) return -1;

        int n = Math.min(out.length, size);
        int first = Math.min(n, buffer.length - readPos);
        System.arraycopy(buffer, readPos, out, 0, first);
        readPos = (readPos + first) % buffer.length;
        size -= first;

        int remain = n - first;
        if (remain > 0) {
            System.arraycopy(buffer, readPos, out, first, remain);
            readPos += remain;
            size -= remain;
        }

        notifyAll();
        return n;
    }

    public synchronized void close() {
        closed = true;
        notifyAll();
        Log.d(TAG, "closed");
    }
}
