package com.github.jaykkumar01.vaultspace.media.cache;

import android.content.Context;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.datasource.cache.Cache;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.core.auth.DriveAuthGate;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@UnstableApi
public final class DriveAltMediaCache {

    private static final String TAG = "DriveAltMediaCache";

    private static final long MAX_CACHE_BYTES = 512L * 1024L * 1024L;
    private static final long SMALL_FILE_THRESHOLD = 5L * 1024L * 1024L;

    private static final int HEADER_BYTES = 64;
    private static final int TAIL_BYTES = 64 * 1024;

    private static final int HEADER_CONNECT_TIMEOUT_MS = 1200;
    private static final int HEADER_READ_TIMEOUT_MS = 1200;

    private static final int TAIL_CONNECT_TIMEOUT_MS = 10000;
    private static final int TAIL_READ_TIMEOUT_MS = 5000;

    private static final long HEADER_SPAM_WINDOW_MS = 10_000;

    private static SimpleCache cache;

    private final Context context;

    private final ExecutorService headerExecutor;
    private final ExecutorService tailExecutor;

    private final AtomicBoolean released = new AtomicBoolean(false);

    public DriveAltMediaCache(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.headerExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "DriveWarmUp-Header");
            t.setDaemon(true);
            return t;
        });
        this.tailExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "DriveWarmUp-Tail");
            t.setDaemon(true);
            return t;
        });
    }

    /* ---------------- cache ---------------- */

    private synchronized Cache getCache() {
        if (cache != null) return cache;
        File dir = new File(context.getCacheDir(), "exo_drive_altmedia_cache");
        cache = new SimpleCache(dir, new LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES), new StandaloneDatabaseProvider(context));
        return cache;
    }

    /* ---------------- playback wrap ---------------- */

    public CacheDataSource.Factory wrap(@NonNull String fileId,@NonNull androidx.media3.datasource.DataSource.Factory upstream) {
        return new CacheDataSource.Factory()
                .setCache(getCache())
                .setCacheKeyFactory(spec -> fileId)
                .setUpstreamDataSourceFactory(upstream)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
    }

    /* ---------------- warm-up ---------------- */

    public void warmUpRanges(@NonNull AlbumMedia media) {
        if (released.get() || media.sizeBytes <= 0) return;

        String fileId = media.fileId;
        long size = media.sizeBytes;

        headerExecutor.execute(() -> spamHeaderWarmUp(fileId, size));

        if (size >= SMALL_FILE_THRESHOLD) {
            long tailStart = Math.max(0, size - TAIL_BYTES);
            tailExecutor.execute(() -> probeOnce(fileId, size, tailStart, TAIL_BYTES, false));
        }
    }

    /* ---------------- header spam loop ---------------- */

    private void spamHeaderWarmUp(@NonNull String fileId,long size) {
        long windowStart = SystemClock.elapsedRealtime();
        int attempts = 0;

        Log.d(TAG, "[" + fileId + "] header spam start");

        while (!released.get()
                && !Thread.currentThread().isInterrupted()
                && SystemClock.elapsedRealtime() - windowStart < HEADER_SPAM_WINDOW_MS) {

            attempts++;

            if (probeOnce(fileId, size, 0, HEADER_BYTES, true)) {
                Log.d(TAG, "[" + fileId + "] header spam success attempts=" + attempts +
                        " +" + (SystemClock.elapsedRealtime() - windowStart) + "ms");
                return;
            }
        }

        if (released.get() || Thread.currentThread().isInterrupted()) {
            Log.d(TAG, "[" + fileId + "] header spam aborted");
        } else {
            Log.d(TAG, "[" + fileId + "] header spam timeout attempts=" + attempts);
        }
    }


    /* ---------------- single probe ---------------- */

    private boolean probeOnce(@NonNull String fileId,long size,long position,int maxBytes,boolean isHeader) {
        if (released.get()) return false;

        long start = SystemClock.elapsedRealtime();
        long firstByteAt = -1;
        int totalRead = 0;

        try {
            String token = DriveAuthGate.get(context).getToken();
            Uri uri = Uri.parse("https://www.googleapis.com/drive/v3/files/" + fileId + "?alt=media");

            DefaultHttpDataSource.Factory factory =
                    new DefaultHttpDataSource.Factory()
                            .setConnectTimeoutMs(isHeader ? HEADER_CONNECT_TIMEOUT_MS : TAIL_CONNECT_TIMEOUT_MS)
                            .setReadTimeoutMs(isHeader ? HEADER_READ_TIMEOUT_MS : TAIL_READ_TIMEOUT_MS)
                            .setDefaultRequestProperties(
                                    Map.of(
                                            "Authorization", "Bearer " + token,
                                            "Range", "bytes=" + position + "-"
                                    )
                            );

            HttpDataSource source = factory.createDataSource();
            DataSpec spec = new DataSpec.Builder().setUri(uri).setPosition(position).build();

            byte[] buffer = new byte[8 * 1024];
            int remaining = maxBytes;

            source.open(spec);
            int responseCode = source.getResponseCode();

            while (remaining > 0 && !released.get()) {
                int r = source.read(buffer, 0, Math.min(buffer.length, remaining));
                if (r == -1) break;
                if (firstByteAt == -1) firstByteAt = SystemClock.elapsedRealtime();
                totalRead += r;
                remaining -= r;
            }

            source.close();

            long totalMs = SystemClock.elapsedRealtime() - start;
            long firstByteMs = firstByteAt == -1 ? -1 : (firstByteAt - start);

            Log.d(TAG,
                    "[" + fileId + "] warm-" + (isHeader ? "header" : "tail") +
                            " @" + position +
                            " size=" + size +
                            " resp=" + responseCode +
                            " firstByte=" + firstByteMs + "ms" +
                            " read=" + totalRead +
                            " total=" + totalMs + "ms"
            );

            return responseCode == 206 && totalRead > 0;

        } catch (Exception e) {
            Log.e(TAG,
                    "[" + fileId + "] warm-" + (isHeader ? "header" : "tail") +
                            " FAIL @" + position,
                    e
            );
            return false;
        }
    }

    /* ---------------- release ---------------- */

    public void release() {
        if (!released.compareAndSet(false, true)) return;

        headerExecutor.shutdownNow();
        tailExecutor.shutdownNow();

        Log.d(TAG, "warm-up executors shutdown");
    }
}
