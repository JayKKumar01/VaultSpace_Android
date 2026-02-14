package com.github.jaykkumar01.vaultspace.media.prefetch;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.cache.CacheDataSource;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@UnstableApi
public final class DrivePrefetcher {

    private static final String SCHEME = "vaultspace://drive/";
    private ExecutorService executor = Executors.newFixedThreadPool(2);
    private volatile boolean cancelled;
    private final CacheDataSource.Factory cacheFactory;

    public interface Callback { void onDone(); }

    public DrivePrefetcher(@NonNull CacheDataSource.Factory factory) {
        this.cacheFactory = factory;
    }

    public void prefetch(@NonNull Context ctx, @NonNull AlbumMedia media, @NonNull Callback cb) {
        if (!media.hasStartupLayoutInfo()) { cb.onDone(); return; }
        cancelled = false;
        if (executor.isShutdown() || executor.isTerminated())
            executor = Executors.newFixedThreadPool(2);

        CountDownLatch latch = new CountDownLatch(2);

        executor.execute(() -> { try { fetch(media, 0, media.headRequiredBytes); } finally { latch.countDown(); } });
        executor.execute(() -> { try { fetch(media, media.getTailStartPosition(), media.tailRequiredBytes); } finally { latch.countDown(); } });

        new Thread(() -> {
            try { latch.await(); } catch (Exception ignored) {}
            if (!cancelled) cb.onDone();
        }).start();
    }

    private void fetch(AlbumMedia media, long pos, long len) {
        if (pos < 0 || len <= 0 || cancelled) return;
        CacheDataSource ds = null;
        try {
            ds = cacheFactory.createDataSource();
            DataSpec spec = new DataSpec.Builder()
                    .setUri(SCHEME + media.fileId)
                    .setPosition(pos)
                    .setLength(len)
                    .build();
            ds.open(spec);
            byte[] buf = new byte[16 * 1024];
            long remaining = len;
            while (!cancelled && remaining > 0) {
                int r = ds.read(buf, 0, (int) Math.min(buf.length, remaining));
                if (r == -1) break;
                remaining -= r;
            }
        } catch (Exception ignored) {
        } finally {
            try { if (ds != null) ds.close(); } catch (Exception ignored) {}
        }
    }

    public void cancel() {
        cancelled = true;
        executor.shutdownNow();
    }
}
