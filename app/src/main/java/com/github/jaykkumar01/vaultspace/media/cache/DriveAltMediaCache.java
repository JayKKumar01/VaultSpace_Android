package com.github.jaykkumar01.vaultspace.media.cache;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.media3.common.util.UnstableApi;
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
    private static final long MAX_CACHE_BYTES = 120L * 1024L * 1024L;

    /* ---------------- CACHE ---------------- */

    private static SimpleCache cache;

    /* ---------------- CORE ---------------- */

    private final Context context;

    public DriveAltMediaCache(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    /* ---------------- CACHE INIT ---------------- */

    private synchronized Cache getCache() {
        if (cache != null) return cache;
        File dir = new File(context.getCacheDir(), "exo_drive_altmedia_cache");
        cache = new SimpleCache(
                dir,
                new LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES),
                new StandaloneDatabaseProvider(context)
        );
        return cache;
    }

    /* ---------------- PLAYBACK WRAP ---------------- */

    public CacheDataSource.Factory wrap(@NonNull String fileId,
                                        @NonNull DataSource.Factory upstream) {
        return new CacheDataSource.Factory()
                .setCache(getCache())
                .setCacheKeyFactory(spec -> fileId)
                .setUpstreamDataSourceFactory(upstream)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
    }

    /* ---------------- RELEASE ---------------- */

    public void release() {
        // executor intentionally persistent
        // cache intentionally app-lifetime
    }
}
