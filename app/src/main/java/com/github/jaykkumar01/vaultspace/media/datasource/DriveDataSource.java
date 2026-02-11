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

    private static final String TAG = "Drive:HybridStream";
    private static final int BUFFER_SIZE = 8 * 1024 * 1024;
    private static final int TEMP_READ  = 128 * 1024;

    private final DriveStreamSource source;
    private final AlbumMedia media;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r,"Drive-Reader");
                t.setDaemon(true);
                return t;
            });

    private final Object lock = new Object();
    private final byte[] buffer = new byte[BUFFER_SIZE];

    private int writePos, readPos, available;
    private boolean eof;

    private final AtomicLong generation = new AtomicLong();

    private volatile DriveStreamSource.StreamSession session;
    private volatile Future<?> readerTask;
    private @Nullable Uri uri;

    /* -------- instrumentation -------- */

    private long openPosition;
    private long producedBytes;
    private long consumedBytes;
    private long activeGen;

    public DriveDataSource(Context context, AlbumMedia media) {
        this.media = media;
        Log.d(TAG,"INIT fileId="+media.fileId+" sizeBytes="+media.sizeBytes);
        this.source = new DriveHttpSource(context,media.fileId);
    }

    /* ---------------- OPEN ---------------- */

    @Override
    public long open(DataSpec spec) throws IOException {

        invalidate();

        long myGen = generation.incrementAndGet();
        activeGen = myGen;
        openPosition = spec.position;
        producedBytes = 0;
        consumedBytes = 0;

        uri = spec.uri;

        Log.d(TAG,"OPEN gen=" + myGen + " @" + spec.position);

        session = source.open(spec.position);

        readerTask = executor.submit(() ->
                readerLoop(myGen, session));

        return C.LENGTH_UNSET;
    }

    /* ---------------- READER ---------------- */

    private void readerLoop(long myGen, DriveStreamSource.StreamSession s) {

        byte[] temp = new byte[TEMP_READ];

        try (InputStream in = s.stream()) {

            int r;

            while (myGen == generation.get()
                    && (r = in.read(temp)) != -1) {

                synchronized (lock) {

                    if (myGen != generation.get())
                        return;

                    while (available + r > BUFFER_SIZE) {
                        lock.wait();
                        if (myGen != generation.get())
                            return;
                    }

                    int first = Math.min(r,BUFFER_SIZE - writePos);
                    System.arraycopy(temp,0,buffer,writePos,first);

                    int remain = r - first;
                    if (remain > 0)
                        System.arraycopy(temp,first,buffer,0,remain);

                    writePos = (writePos + r) % BUFFER_SIZE;
                    available += r;

                    producedBytes += r;

                    lock.notify();
                }
            }

        } catch (Exception e) {

            if (myGen == generation.get())
                Log.e(TAG,"reader error gen=" + myGen,e);
            else
                Log.d(TAG,"reader obsolete gen=" + myGen);

        } finally {

            s.cancel();

            synchronized (lock) {
                if (myGen == generation.get()) {
                    eof = true;
                    lock.notify();
                }
            }

            Log.d(TAG,"READER END gen=" + myGen +
                    " produced=" + producedBytes);
        }
    }

    /* ---------------- READ ---------------- */

    @Override
    public int read(@NonNull byte[] target,int offset,int length) {

        synchronized (lock) {

            while (available == 0 && !eof) {
                try { lock.wait(); } catch (InterruptedException ignored) {}
            }

            if (available == 0)
                return C.RESULT_END_OF_INPUT;

            int toRead = Math.min(length,available);

            int first = Math.min(toRead,BUFFER_SIZE - readPos);
            System.arraycopy(buffer,readPos,target,offset,first);

            int remain = toRead - first;
            if (remain > 0)
                System.arraycopy(buffer,0,target,offset + first,remain);

            readPos = (readPos + toRead) % BUFFER_SIZE;
            available -= toRead;

            consumedBytes += toRead;

            lock.notify();
            return toRead;
        }
    }

    /* ---------------- INVALIDATE ---------------- */

    private void invalidate() {

        long closingGen = activeGen;
        long wasted = producedBytes - consumedBytes;
        int bufferedAtClose = available;

        if (closingGen != 0) {
            Log.d(TAG,
                    "CLOSE gen=" + closingGen +
                            " open@" + openPosition +
                            " consumed=" + consumedBytes +
                            " produced=" + producedBytes +
                            " wasted=" + wasted +
                            " buffered=" + bufferedAtClose);
        }

        generation.incrementAndGet();

        synchronized (lock) {
            writePos = readPos = available = 0;
            eof = false;
            lock.notifyAll();
        }

        if (readerTask != null)
            readerTask.cancel(true);

        if (session != null)
            session.cancel();

        readerTask = null;
        session = null;
        activeGen = 0;
    }

    /* ---------------- CLOSE ---------------- */

    @Override
    public void close() {
        invalidate();
        uri = null;
    }

    /* ---------------- MISC ---------------- */

    @Override public void addTransferListener(@NonNull TransferListener l) {}
    @Override public @Nullable Uri getUri() { return uri; }
    @Override public @NonNull Map<String,List<String>> getResponseHeaders() {
        return Collections.emptyMap();
    }

    public void release() {
        close();
    }
}
