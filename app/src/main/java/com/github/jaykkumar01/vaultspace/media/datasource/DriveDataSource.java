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

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@UnstableApi
public final class DriveDataSource implements DataSource {

    private static final String TAG = "Drive:HybridCache";
    private static final int READ_CHUNK = 128 * 1024;

    /* ---------------- Core ---------------- */

    private final DriveStreamSource source;
    private final long sizeBytes;

    /* ---------------- Shared Memory Buffer ---------------- */

    private final byte[] buffer;
    private final TreeSet<Range> ranges = new TreeSet<>();
    private final Object lock = new Object();

    /* ---------------- Downloaders ---------------- */

    private final ExecutorService mainExecutor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r,"Drive-Main");
                t.setDaemon(true);
                return t;
            });

    private final ExecutorService tempExecutor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r,"Drive-Temp");
                t.setDaemon(true);
                return t;
            });

    private Future<?> mainTask;
    private Future<?> tempTask;

    private DriveStreamSource.StreamSession mainSession;
    private DriveStreamSource.StreamSession tempSession;

    /* ---------------- State ---------------- */

    private long readPosition = 0;
    private volatile boolean released = false;
    private volatile boolean mainFinished = false;

    private @Nullable Uri uri;

    /* ============================================================= */
    /* ========================= CONSTRUCTOR ======================== */
    /* ============================================================= */

    public DriveDataSource(Context context,String fileId,long sizeBytes) {
        this.source = new DriveHttpSource(context,fileId);
        this.sizeBytes = sizeBytes;
        this.buffer = new byte[(int) sizeBytes];
        startMainDownloader();
    }

    /* ============================================================= */
    /* ====================== MAIN DOWNLOADER ======================= */
    /* ============================================================= */

    private void startMainDownloader() {

        mainTask = mainExecutor.submit(() -> {

            long offset = 0;

            try {
                mainSession = source.open(0);

                try (InputStream in = mainSession.stream()) {

                    byte[] temp = new byte[READ_CHUNK];
                    int r;

                    while (!released && (r = in.read(temp)) != -1) {

                        writeToBuffer(offset,temp,r);
                        offset += r;
                    }

                    mainFinished = true;
                    Log.d(TAG,"Main downloader finished");

                    // 🔥 Immediately cancel temp when full file available
                    cancelTemp();

                }

            } catch (Exception e) {
                if (!released)
                    Log.e(TAG,"Main downloader error",e);
            }
        });
    }

    /* ============================================================= */
    /* ======================= TEMP DOWNLOADER ====================== */
    /* ============================================================= */

    private void startTempDownloader(long position) {

        if (mainFinished || released)
            return;

        cancelTemp();

        tempTask = tempExecutor.submit(() -> {

            long offset = position;

            try {
                tempSession = source.open(position);

                try (InputStream in = tempSession.stream()) {

                    byte[] temp = new byte[READ_CHUNK];
                    int r;

                    while (!released
                            && !mainFinished
                            && (r = in.read(temp)) != -1) {

                        if (isRangeFilled(offset,r))
                            break;

                        writeToBuffer(offset,temp,r);
                        offset += r;
                    }

                }

            } catch (Exception e) {
                if (!released && !mainFinished)
                    Log.e(TAG,"Temp downloader error",e);
            }
        });
    }

    private void cancelTemp() {

        if (tempTask != null)
            tempTask.cancel(true);

        if (tempSession != null)
            tempSession.cancel();

        tempTask = null;
        tempSession = null;
    }

    /* ============================================================= */
    /* ============================ OPEN ============================ */
    /* ============================================================= */

    @Override
    public long open(DataSpec spec) {

        readPosition = spec.position;
        uri = spec.uri;

        if (!isByteAvailable(readPosition))
            startTempDownloader(readPosition);

        return C.LENGTH_UNSET;
    }

    /* ============================================================= */
    /* ============================= READ =========================== */
    /* ============================================================= */

    @Override
    public int read(@NonNull byte[] target,int offset,int length) {

        synchronized (lock) {

            while (!released && !isByteAvailable(readPosition)) {
                try { lock.wait(); } catch (InterruptedException ignored) {}
            }

            if (released || readPosition >= sizeBytes)
                return C.RESULT_END_OF_INPUT;

            long available = contiguousAvailable(readPosition);
            int toRead = (int) Math.min(length,available);

            System.arraycopy(buffer,(int) readPosition,target,offset,toRead);

            readPosition += toRead;

            return toRead;
        }
    }

    /* ============================================================= */
    /* ======================== BUFFER LOGIC ======================== */
    /* ============================================================= */

    private void writeToBuffer(long start,byte[] src,int len) {

        synchronized (lock) {

            System.arraycopy(src,0,buffer,(int) start,len);
            mergeRange(start,start + len);

            lock.notifyAll();
        }
    }

    private boolean isByteAvailable(long position) {
        for (Range r : ranges)
            if (r.start <= position && r.end > position)
                return true;
        return false;
    }

    private long contiguousAvailable(long position) {
        for (Range r : ranges)
            if (r.start <= position && r.end > position)
                return r.end - position;
        return 0;
    }

    private boolean isRangeFilled(long start,int len) {
        long end = start + len;
        for (Range r : ranges)
            if (r.start <= start && r.end >= end)
                return true;
        return false;
    }

    private void mergeRange(long start,long end) {

        Range newRange = new Range(start,end);

        Range lower = ranges.floor(newRange);
        if (lower != null && lower.end >= start) {
            newRange.start = Math.min(lower.start,start);
            newRange.end = Math.max(lower.end,end);
            ranges.remove(lower);
        }

        while (true) {
            Range higher = ranges.ceiling(newRange);
            if (higher != null && higher.start <= newRange.end) {
                newRange.end = Math.max(newRange.end,higher.end);
                ranges.remove(higher);
            } else break;
        }

        ranges.add(newRange);
    }

    /* ============================================================= */
    /* ============================= CLOSE ========================== */
    /* ============================================================= */

    @Override
    public void close() {
        uri = null;
    }

    public void release() {

        released = true;

        cancelTemp();

        if (mainTask != null)
            mainTask.cancel(true);

        if (mainSession != null)
            mainSession.cancel();

        mainExecutor.shutdownNow();
        tempExecutor.shutdownNow();

        synchronized (lock) {
            ranges.clear();
            lock.notifyAll();
        }

        Log.d(TAG,"Released");
    }

    /* ============================================================= */
    /* ============================== MISC ========================== */
    /* ============================================================= */

    @Override public void addTransferListener(@NonNull TransferListener l) {}
    @Override public @Nullable Uri getUri() { return uri; }
    @Override public @NonNull Map<String,List<String>> getResponseHeaders() {
        return Collections.emptyMap();
    }

    /* ============================================================= */
    /* ============================== RANGE ========================= */
    /* ============================================================= */

    private static final class Range implements Comparable<Range> {
        long start;
        long end;
        Range(long s,long e){ start = s; end = e; }
        @Override public int compareTo(@NonNull Range o){
            return Long.compare(this.start,o.start);
        }
    }
}
