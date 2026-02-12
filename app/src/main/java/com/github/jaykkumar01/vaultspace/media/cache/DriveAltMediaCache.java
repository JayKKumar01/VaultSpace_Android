package com.github.jaykkumar01.vaultspace.media.cache;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.database.DatabaseProvider;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.cache.Cache;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;

import java.io.File;

@UnstableApi
public final class DriveAltMediaCache {

    /* ---------------- CONSTANTS ---------------- */

    private static final String TAG = "DriveAltMediaCache";
    private static final String CACHE_DIR_NAME = "exo_drive_altmedia_cache";
    private static final long MAX_CACHE_BYTES = 120L * 1024L * 1024L;

    /* ---------------- SINGLETON CACHE ---------------- */

    private static SimpleCache cache;

    /* ---------------- CORE ---------------- */

    private final Context context;

    public DriveAltMediaCache(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    /* ---------------- CACHE INIT ---------------- */

    private synchronized Cache getCache() {
        if (cache != null) return cache;

        File dir = new File(context.getCacheDir(), CACHE_DIR_NAME);

        Log.d(TAG, "Initializing cache at: " + dir.getAbsolutePath());
        Log.d(TAG, "Max cache size: " + (MAX_CACHE_BYTES / (1024 * 1024)) + " MB");

        DatabaseProvider db = new StandaloneDatabaseProvider(context);
        cache = new SimpleCache(
                dir,
                new LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES),
                db
        );

        return cache;
    }

    /* ---------------- CACHE DATASOURCE WRAP ---------------- */

    public CacheDataSource.Factory wrap(@NonNull String fileId,
                                        @NonNull DataSource.Factory upstream) {

        Log.d(TAG, "CacheDataSource.Factory created for key=" + fileId);

        return new CacheDataSource.Factory()
                .setCache(getCache())
                .setCacheKeyFactory(spec -> fileId) // stable per media file
                .setUpstreamDataSourceFactory(upstream)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
    }

    /* ---------------- RELEASE ---------------- */

    public void release() {
        // cache intentionally process-lifetime
        // do NOT release to preserve data
    }
}
