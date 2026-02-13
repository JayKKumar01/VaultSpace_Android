package com.github.jaykkumar01.vaultspace.media.controller;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.cache.CacheDataSource;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@UnstableApi
public final class DrivePrefetchCoordinator {

    private static final String TAG = "DrivePrefetch";
    private static final String SCHEME = "vaultspace://drive/";

    private final AlbumMedia media;
    private final CacheDataSource.Factory cacheFactory;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private volatile boolean cancelled;

    public interface Callback { void onPrefetchComplete(); }

    public DrivePrefetchCoordinator(@NonNull AlbumMedia media,
                                    @NonNull CacheDataSource.Factory cacheFactory) {
        this.media = media;
        this.cacheFactory = cacheFactory;
    }

    public void start(@NonNull Callback callback) {

        if (!media.hasStartupLayoutInfo()) {
            callback.onPrefetchComplete();
            return;
        }

        executor.execute(() -> {

            if (cancelled) return;

            prefetch(0, media.headRequiredBytes);

            if (cancelled) return;

            prefetch(media.getTailStartPosition(), media.tailRequiredBytes);

            if (cancelled) return;

            callback.onPrefetchComplete();
        });
    }

    private void prefetch(long position, long length) {

        if (position < 0 || length <= 0) return;

        Log.d(TAG, "Prefetch " + position + " → " + (position + length));

        CacheDataSource ds = null;

        try {
            ds = cacheFactory.createDataSource();

            DataSpec spec = new DataSpec.Builder()
                    .setUri(SCHEME + media.fileId)
                    .setPosition(position)
                    .setLength(length)
                    .build();

            ds.open(spec);

            byte[] buffer = new byte[16 * 1024];
            long remaining = length;

            while (!cancelled && remaining > 0) {
                int read = ds.read(buffer, 0,
                        (int) Math.min(buffer.length, remaining));
                if (read == -1) break;
                remaining -= read;
            }

        } catch (Exception e) {
            Log.e(TAG, "Prefetch error", e);
        } finally {
            try { if (ds != null) ds.close(); } catch (Exception ignored) {}
        }
    }

    public void cancel() {
        cancelled = true;
        executor.shutdownNow();
        Log.d(TAG, "Cancelled");
    }
}
