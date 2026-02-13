package com.github.jaykkumar01.vaultspace.media.controller;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.cache.CacheDataSource;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.media.cache.DriveAltMediaCache;
import com.github.jaykkumar01.vaultspace.media.datasource.DriveDataSource;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@UnstableApi
public final class DrivePrefetchCoordinator {

    private static final String TAG = "DrivePrefetchCoordinator";
    private static final String SCHEME = "vaultspace://drive/";

    /* ---------------- CORE ---------------- */

    private final AlbumMedia media;
    private final CacheDataSource.Factory cacheFactory;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private volatile boolean cancelled;

    /* ---------------- CALLBACK ---------------- */

    public interface Callback {
        void onHeadPrefetchComplete();
    }

    /* ---------------- CONSTRUCTOR ---------------- */

    public DrivePrefetchCoordinator(
            @NonNull AlbumMedia media,
            @NonNull CacheDataSource.Factory cacheFactory
    ) {
        this.media = media;
        this.cacheFactory = cacheFactory;
    }


    /* ---------------- START ---------------- */

    public void start(@NonNull Callback callback) {

        // If no optimization data → skip prefetch entirely
        if (!media.hasStartupLayoutInfo()) {
            callback.onHeadPrefetchComplete();
            return;
        }

        // HEAD must complete before preparePlayer()
        executor.execute(() -> {
            prefetchHead();
            if (!cancelled) callback.onHeadPrefetchComplete();
        });

        // TAIL can run independently
        if (media.tailRequiredBytes > 0) {
            executor.execute(this::prefetchTail);
        }
    }

    /* ---------------- HEAD PREFETCH ---------------- */

    private void prefetchHead() {
        long length = media.headRequiredBytes;
        if (length <= 0) return;

        Log.d(TAG, "Prefetch HEAD 0 → " + length);
        prefetchRange(0, length);
        Log.d(TAG, "HEAD complete");
    }

    /* ---------------- TAIL PREFETCH ---------------- */

    private void prefetchTail() {
        long start = media.getTailStartPosition();
        long length = media.tailRequiredBytes;

        if (start < 0 || length <= 0) return;

        Log.d(TAG, "Prefetch TAIL " + start + " → " + (start + length));
        prefetchRange(start, length);
        Log.d(TAG, "TAIL complete");
    }

    /* ---------------- RANGE PREFETCH ---------------- */

    private void prefetchRange(long position, long length) {

        CacheDataSource dataSource = null;

        try {
            dataSource = cacheFactory.createDataSource();

            DataSpec spec = new DataSpec.Builder()
                    .setUri(SCHEME + media.fileId)
                    .setPosition(position)
                    .setLength(length)
                    .build();

            dataSource.open(spec);

            byte[] buffer = new byte[16 * 1024];
            long remaining = length;

            while (!cancelled && remaining > 0) {
                int read = dataSource.read(buffer, 0,
                        (int) Math.min(buffer.length, remaining));
                if (read == -1) break;
                remaining -= read;
            }

        } catch (Exception e) {
            Log.e(TAG, "Prefetch error", e);
        } finally {
            try {
                if (dataSource != null)
                    dataSource.close();
            } catch (Exception ignored) {}
        }
    }

    /* ---------------- CANCEL ---------------- */

    public void cancel() {
        cancelled = true;
        executor.shutdownNow();
        Log.d(TAG, "Cancelled");
    }
}
