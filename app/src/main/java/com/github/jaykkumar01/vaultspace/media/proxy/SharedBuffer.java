package com.github.jaykkumar01.vaultspace.media.proxy;

public final class SharedBuffer {

    private final byte[] buffer;
    private final int capacity;

    private int writePos = 0;
    private int readPos = 0;
    private int size = 0;

    private boolean closed = false;

    public SharedBuffer(int maxBytes) {
        this.capacity = maxBytes;
        this.buffer = new byte[maxBytes];
    }

    public synchronized void write(byte[] src, int len) throws InterruptedException {
        int offset = 0;
        while (offset < len) {
            while (size == capacity && !closed) wait();
            if (closed) return;

            int space = capacity - size;
            int toWrite = Math.min(space, len - offset);

            int first = Math.min(toWrite, capacity - writePos);
            System.arraycopy(src, offset, buffer, writePos, first);
            writePos = (writePos + first) % capacity;
            size += first;
            offset += first;

            int remaining = toWrite - first;
            if (remaining > 0) {
                System.arraycopy(src, offset, buffer, writePos, remaining);
                writePos += remaining;
                size += remaining;
                offset += remaining;
            }

            notifyAll();
        }
    }

    public synchronized int read(byte[] out) throws InterruptedException {
        while (size == 0 && !closed) wait();
        if (size == 0) return -1;

        int toRead = Math.min(out.length, size);

        int first = Math.min(toRead, capacity - readPos);
        System.arraycopy(buffer, readPos, out, 0, first);
        readPos = (readPos + first) % capacity;
        size -= first;

        int remaining = toRead - first;
        if (remaining > 0) {
            System.arraycopy(buffer, readPos, out, first, remaining);
            readPos += remaining;
            size -= remaining;
        }

        notifyAll();
        return toRead;
    }

    public synchronized void clear() {
        size = 0;
        readPos = 0;
        writePos = 0;
        notifyAll();
    }

    public synchronized void close() {
        closed = true;
        notifyAll();
    }
}
