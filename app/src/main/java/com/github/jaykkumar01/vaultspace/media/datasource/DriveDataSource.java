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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@UnstableApi
public final class DriveDataSource implements DataSource {

    private static final String TAG = "Drive:HybridStream";

    private static final int BUFFER_SIZE = 8 * 1024 * 1024;
    private static final int TEMP_READ  = 128 * 1024;

    private final DriveSource driveSource;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r,"Drive-Reader");
                t.setDaemon(true);
                return t;
            });

    private final Object lock = new Object();
    private final byte[] buffer = new byte[BUFFER_SIZE];

    private int writePos, readPos, available;

    private final AtomicLong session = new AtomicLong();
    private final AtomicBoolean cancelled = new AtomicBoolean(true);

    private volatile boolean eof;
    private volatile Future<?> readerTask;
    private volatile InputStream stream;
    private @Nullable Uri uri;

    public DriveDataSource(Context context,String fileId,long sizeBytes) {
        this.driveSource = new DriveHttpSource(context,fileId);
    }

    /* ---------------- HARD RESET ---------------- */

    private void hardReset() {

        cancelled.set(true);
        session.incrementAndGet();

        synchronized (lock) {
            writePos = readPos = available = 0;
            eof = false;
            lock.notifyAll();
        }

        try { if (stream != null) stream.close(); }
        catch (Exception ignored) {}

        driveSource.close();

        if (readerTask != null)
            readerTask.cancel(true);

        stream = null;
        readerTask = null;

        Log.d(TAG,"hard reset");
    }

    /* ---------------- open ---------------- */

    @Override
    public long open(DataSpec spec) throws IOException {

        hardReset(); // 💥 instant queue clear on every open

        uri = spec.uri;
        cancelled.set(false);

        long mySession = session.get();
        long position = spec.position;

        Log.d(TAG,"open @" + position);

        stream = driveSource.openStream(position);
        readerTask = executor.submit(() -> readerLoop(mySession));

        return C.LENGTH_UNSET;
    }

    /* ---------------- reader ---------------- */

    private void readerLoop(long mySession) {

        byte[] temp = new byte[TEMP_READ];

        try {
            int r;

            while (!cancelled.get()
                    && mySession == session.get()
                    && !Thread.currentThread().isInterrupted()
                    && (r = stream.read(temp)) != -1) {

                synchronized (lock) {

                    if (cancelled.get() || mySession != session.get())
                        return;

                    while (available + r > BUFFER_SIZE) {
                        lock.wait();
                        if (cancelled.get() || mySession != session.get())
                            return;
                    }

                    int first = Math.min(r,BUFFER_SIZE - writePos);
                    System.arraycopy(temp,0,buffer,writePos,first);

                    int remain = r - first;
                    if (remain > 0)
                        System.arraycopy(temp,first,buffer,0,remain);

                    writePos = (writePos + r) % BUFFER_SIZE;
                    available += r;

                    lock.notify();
                }
            }

        } catch (Exception e) {

            if (!cancelled.get() && mySession == session.get())
                Log.e(TAG,"reader error",e);
            else
                Log.d(TAG,"reader cancelled");

        } finally {
            synchronized (lock) {
                if (mySession == session.get()) {
                    eof = true;
                    lock.notify();
                }
            }
        }
    }

    /* ---------------- read ---------------- */

    @Override
    public int read(@NonNull byte[] target,int offset,int length) {

        synchronized (lock) {

            while (!cancelled.get() && available == 0 && !eof) {
                try { lock.wait(); } catch (InterruptedException ignored) {}
            }

            if (available == 0 && eof)
                return C.RESULT_END_OF_INPUT;

            int toRead = Math.min(length,available);

            int first = Math.min(toRead,BUFFER_SIZE - readPos);
            System.arraycopy(buffer,readPos,target,offset,first);

            int remain = toRead - first;
            if (remain > 0)
                System.arraycopy(buffer,0,target,offset + first,remain);

            readPos = (readPos + toRead) % BUFFER_SIZE;
            available -= toRead;

            lock.notify();
            return toRead;
        }
    }

    /* ---------------- close ---------------- */

    @Override
    public void close() {
        hardReset();
        uri = null;
    }

    /* ---------------- misc ---------------- */

    @Override public void addTransferListener(@NonNull TransferListener l) {}
    @Override public @Nullable Uri getUri() { return uri; }
    @Override public @NonNull Map<String,List<String>> getResponseHeaders() {
        return Collections.emptyMap();
    }
}

