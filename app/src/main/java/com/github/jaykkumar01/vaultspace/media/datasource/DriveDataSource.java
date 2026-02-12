package com.github.jaykkumar01.vaultspace.media.datasource;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.TransferListener;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

@UnstableApi
public final class DriveDataSource implements DataSource {

    /* ========================= CONSTANTS ========================= */

    private static final String TAG = "Drive:HybridStream";
    private static final int BUFFER_SIZE = 512 * 1024;
    private static final int TEMP_READ = 128 * 1024;

    /* ========================= STATE ========================= */

    private enum State { IDLE, OPEN }
    private volatile State state = State.IDLE;

    private synchronized void transitionTo(State next) throws IOException {
        if (state == next) return;
        State prev = state;
        onExit(prev);
        state = next;
        onEnter(next);
        Log.d(TAG, "STATE " + prev + " → " + next);
    }

    private void onExit(State from) {
        if (from == State.OPEN) stopSession();
    }

    private void onEnter(State s) throws IOException {
        if (s == State.IDLE) resetBuffer();
        if (s == State.OPEN) startSession(openSpec.position);
    }

    /* ========================= CORE ========================= */

    private final DriveStreamSource source;
    private final AlbumMedia media;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Drive-Reader");
        t.setDaemon(true);
        return t;
    });

    /* ========================= BUFFER ========================= */

    private final Object lock = new Object();
    private final byte[] buffer = new byte[BUFFER_SIZE];
    private int writePos, readPos, available;
    private boolean eof;

    /* ========================= SESSION ========================= */

    private final AtomicLong generation = new AtomicLong();
    private volatile long activeGen;
    private volatile DriveStreamSource.StreamSession session;
    private volatile Future<?> readerTask;

    /* ========================= OPEN CONTEXT ========================= */

    private DataSpec openSpec;
    private @Nullable Uri uri;

    /* ========================= METRICS ========================= */

    private long openPosition, producedBytes, consumedBytes;

    /* ========================= CONSTRUCTOR ========================= */

    public DriveDataSource(Context context, AlbumMedia media) {
        this.media = media;
        this.source = new DriveHttpSource(context, media.fileId);
        Log.d(TAG, "INIT fileId=" + media.fileId + " size=" + media.sizeBytes);
    }

    /* ========================= OPEN ========================= */

    @Override
    public long open(DataSpec spec) throws IOException {
        uri = spec.uri;
        openSpec = spec;
        transitionTo(State.IDLE);
        transitionTo(State.OPEN);
        return session != null ? session.length() : C.LENGTH_UNSET;
    }

    /* ========================= SESSION CONTROL ========================= */

    private void startSession(long position) throws IOException {
        openPosition = position;
        producedBytes = consumedBytes = 0;

        long gen = generation.incrementAndGet();
        activeGen = gen;

        session = source.open(position);
        readerTask = executor.submit(() -> readerLoop(gen, session));

        Log.d(TAG, "SESSION START gen=" + gen + " @" + position);
    }

    private void stopSession() {

        if (activeGen == 0) return;

        logCloseStats();

        /* ----- phase 1: invalidate generation ----- */

        generation.incrementAndGet();
        activeGen = 0;

        synchronized (lock) {
            eof = true;
            lock.notifyAll();
        }

        /* ----- phase 2: wait reader graceful exit ----- */

        Future<?> task = readerTask;
        if (task != null) {
            try { task.get(); } catch (Exception ignored) {}
        }

        /* ----- phase 3: close upstream ----- */

        DriveStreamSource.StreamSession s = session;
        if (s != null) {
            try { s.cancel(); } catch (Exception ignored) {}
        }

        readerTask = null;
        session = null;
    }

    /* ========================= READER ========================= */

    private void readerLoop(long gen, DriveStreamSource.StreamSession s) {

        byte[] temp = new byte[TEMP_READ];

        try (InputStream in = s.stream()) {

            int r;
            while (gen == generation.get() && (r = in.read(temp)) != -1) {
                writeBlocking(gen, temp, r);
            }

        } catch (Exception e) {
            if (gen == generation.get())
                Log.e(TAG, "reader error gen=" + gen, e);
        } finally {
            signalEof(gen);
            Log.d(TAG, "READER END gen=" + gen + " produced=" + producedBytes);
        }
    }

    /* ========================= BUFFER WRITE ========================= */

    private void writeBlocking(long gen, byte[] src, int len) throws InterruptedException {

        synchronized (lock) {

            if (gen != generation.get()) return;

            while (available + len > BUFFER_SIZE) {
                if (gen != generation.get()) return;
                lock.wait();
            }

            int first = Math.min(len, BUFFER_SIZE - writePos);
            System.arraycopy(src, 0, buffer, writePos, first);

            int remain = len - first;
            if (remain > 0) System.arraycopy(src, first, buffer, 0, remain);

            writePos = (writePos + len) % BUFFER_SIZE;
            available += len;
            producedBytes += len;

            lock.notify();
        }
    }

    /* ========================= READ ========================= */

    @Override
    public int read(@NonNull byte[] target, int offset, int length) {

        synchronized (lock) {

            while (available == 0 && !eof) {
                try { lock.wait(); }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return C.RESULT_END_OF_INPUT;
                }
            }

            if (available == 0) return C.RESULT_END_OF_INPUT;

            int toRead = Math.min(length, available);

            int first = Math.min(toRead, BUFFER_SIZE - readPos);
            System.arraycopy(buffer, readPos, target, offset, first);

            int remain = toRead - first;
            if (remain > 0) System.arraycopy(buffer, 0, target, offset + first, remain);

            readPos = (readPos + toRead) % BUFFER_SIZE;
            available -= toRead;
            consumedBytes += toRead;

            lock.notify();
            return toRead;
        }
    }

    /* ========================= SUPPORT ========================= */

    private void signalEof(long gen) {
        synchronized (lock) {
            if (gen == generation.get()) {
                eof = true;
                lock.notifyAll();
            }
        }
    }

    private void resetBuffer() {
        synchronized (lock) {
            writePos = readPos = available = 0;
            eof = false;
            lock.notifyAll();
        }
    }

    private void logCloseStats() {
        Log.d(TAG,
                "CLOSE gen=" + activeGen +
                        " open@" + openPosition +
                        " consumed=" + consumedBytes +
                        " produced=" + producedBytes +
                        " wasted=" + (producedBytes - consumedBytes) +
                        " buffered=" + available
        );
    }

    /* ========================= CLOSE ========================= */

    @Override
    public void close() {
        try { transitionTo(State.IDLE); } catch (IOException ignored) {}
        uri = null;
    }

    /* ========================= MISC ========================= */

    @Override public void addTransferListener(@NonNull TransferListener l) {}
    @Override public @Nullable Uri getUri() { return uri; }
    @Override public @NonNull Map<String,List<String>> getResponseHeaders() { return Collections.emptyMap(); }
}
