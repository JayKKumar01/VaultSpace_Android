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
import com.github.jaykkumar01.vaultspace.media.datasource.DrivePrefetchDataSource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@UnstableApi
public final class DrivePrefetchCoordinator {

    private static final String TAG = "DrivePrefetch";
    private static final String SCHEME = "vaultspace://drive/";

    /* ---------------- CORE ---------------- */

    private final Context context;
    private final AlbumMedia media;
    private final DriveAltMediaCache cache;

    /* ---------------- EXECUTION ---------------- */

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    /* ---------------- STATE ---------------- */

    private volatile boolean cancelled;

    public interface Callback { void onPrefetchComplete(); }

    public DrivePrefetchCoordinator(@NonNull Context context,
                                    @NonNull AlbumMedia media,
                                    @NonNull DriveAltMediaCache cache) {
        this.context = context.getApplicationContext();
        this.media = media;
        this.cache = cache;
    }

    /* ============================================================= */
    /* =========================== START ============================ */
    /* ============================================================= */

    public void start(@NonNull Callback callback) {

        if (!media.hasStartupLayoutInfo()) {
            callback.onPrefetchComplete();
            return;
        }

        CountDownLatch latch = new CountDownLatch(2);

        executor.execute(() -> { try { prefetchRange(0L, media.headRequiredBytes); }
        finally { latch.countDown(); } });

        executor.execute(() -> { try { prefetchRange(media.getTailStartPosition(), media.tailRequiredBytes); }
        finally { latch.countDown(); } });

        // Wait outside executor threads
        new Thread(() -> {
            try { latch.await(); } catch (InterruptedException ignored) {}
            if (!cancelled) callback.onPrefetchComplete();
        }).start();
    }

    /* ============================================================= */
    /* ========================= PREFETCH =========================== */
    /* ============================================================= */

    private void prefetchRange(long position, long length) {

        if (position < 0 || length <= 0 || cancelled) return;

        Log.d(TAG, "Prefetch " + position + " → " + (position + length));

        DrivePrefetchDataSource upstream =
                new DrivePrefetchDataSource(context, media.fileId);

        DataSource.Factory upstreamFactory = () -> upstream;
        CacheDataSource.Factory factory = cache.wrap(media.fileId, upstreamFactory);

        CacheDataSource ds = null;

        try {
            ds = factory.createDataSource();

            DataSpec spec = new DataSpec.Builder()
                    .setUri(SCHEME + media.fileId)
                    .setPosition(position)
                    .setLength(length)
                    .build();

            ds.open(spec);

            byte[] buffer = new byte[16 * 1024];
            long remaining = length;

            while (!cancelled && remaining > 0) {
                int read = ds.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read == -1) break;
                remaining -= read;
            }

        } catch (Exception e) {
            Log.e(TAG, "Prefetch error @" + position, e);
        } finally {
            try { if (ds != null) ds.close(); } catch (Exception ignored) {}
        }
    }

    /* ============================================================= */
    /* =========================== CANCEL =========================== */
    /* ============================================================= */

    public void cancel() {
        cancelled = true;
        executor.shutdownNow();
        Log.d(TAG, "Cancelled");
    }
}
