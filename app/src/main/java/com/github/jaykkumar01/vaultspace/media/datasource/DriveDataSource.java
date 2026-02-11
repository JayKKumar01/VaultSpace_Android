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
    private static final int BUFFER_SIZE = 2 * 1024 * 1024;   // 2MB ring buffer
    private static final int TEMP_READ  = 32 * 1024;

    /* ---------------- core ---------------- */

    private final DriveSource driveSource;
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "Drive-Reader");
                t.setDaemon(true);
                return t;
            });

    /* ---------------- buffer ---------------- */

    private final Object lock = new Object();
    private final byte[] buffer = new byte[BUFFER_SIZE];

    private int writePos, readPos, available;

    /* ---------------- state ---------------- */

    private final AtomicLong session = new AtomicLong(0);
    private final AtomicBoolean closed = new AtomicBoolean(true);

    private volatile boolean eof;
    private volatile Future<?> readerTask;
    private volatile InputStream stream;

    private @Nullable Uri uri;

    /* ---------------- constructor ---------------- */

    public DriveDataSource(Context context, String fileId, long sizeBytes) {
        this.driveSource = new DriveSdkSource(context,fileId);
    }

    /* ---------------- open ---------------- */

    @Override
    public long open(DataSpec spec) throws IOException {

        close(); // ensure previous session fully stopped

        uri = spec.uri;
        closed.set(false);
        eof = false;

        long mySession = session.incrementAndGet();
        long position = spec.position;

        Log.d(TAG,"open @" + position);

        stream = driveSource.openStream(position);

        readerTask = executor.submit(() ->
                readerLoop(mySession)
        );

        return C.LENGTH_UNSET;
    }

    /* ---------------- reader ---------------- */

    private void readerLoop(long mySession) {

        byte[] temp = new byte[TEMP_READ];

        try {
            int r;

            while (!closed.get()
                    && mySession == session.get()
                    && (r = stream.read(temp)) != -1) {

                writeToBuffer(temp,r,mySession);
            }

        } catch (Exception e) {
            if (!closed.get())
                Log.e(TAG,"reader error",e);
        }

        signalEof(mySession);
    }

    private void writeToBuffer(byte[] data,int len,long mySession)
            throws InterruptedException {

        synchronized (lock) {

            while (!closed.get()
                    && mySession == session.get()
                    && available + len > BUFFER_SIZE) {

                lock.wait();
            }

            if (closed.get() || mySession != session.get())
                return;

            int first = Math.min(len,BUFFER_SIZE - writePos);
            System.arraycopy(data,0,buffer,writePos,first);

            int remain = len - first;
            if (remain > 0)
                System.arraycopy(data,first,buffer,0,remain);

            writePos = (writePos + len) % BUFFER_SIZE;
            available += len;

            lock.notifyAll();
        }
    }

    private void signalEof(long mySession) {
        synchronized (lock) {
            if (mySession == session.get()) {
                eof = true;
                lock.notifyAll();
            }
        }
    }

    /* ---------------- read ---------------- */

    @Override
    public int read(@NonNull byte[] target,int offset,int length) {

        synchronized (lock) {

            while (!closed.get() && available == 0 && !eof) {
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

            lock.notifyAll();
            return toRead;
        }
    }

    /* ---------------- close ---------------- */

    @Override
    public void close() {

        if (closed.get()) return;

        Log.d(TAG,"close");

        closed.set(true);
        session.incrementAndGet(); // invalidate previous reader

        synchronized (lock) {
            lock.notifyAll();
        }

        try { if (stream != null) stream.close(); }
        catch (Exception ignored) {}

        driveSource.close();

        if (readerTask != null)
            readerTask.cancel(true);

        resetBuffer();
        uri = null;
    }

    private void resetBuffer() {
        writePos = readPos = available = 0;
        eof = false;
    }

    /* ---------------- misc ---------------- */

    @Override public void addTransferListener(@NonNull TransferListener l) {}
    @Override public @Nullable Uri getUri() { return uri; }
    @Override public @NonNull Map<String,List<String>> getResponseHeaders() { return Collections.emptyMap(); }
}
