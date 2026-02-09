package com.github.jaykkumar01.vaultspace.media.helper;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.database.DatabaseProvider;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.CacheKeyFactory;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;

import java.io.File;

@UnstableApi
public final class DriveSingleFileCacheHelper {

    private static final String TAG = "DriveExoCache";
    private static final long MAX_CACHE_BYTES = 120L * 1024L * 1024L;

    private static SimpleCache cache;

    private DriveSingleFileCacheHelper() {}

    public static synchronized DataSource.Factory wrap(
            @NonNull Context context,
            @NonNull String fileId,
            @NonNull DataSource.Factory upstream) {

        if (cache == null) {
            File dir = new File(context.getCacheDir(), "exo_drive_single_file_cache");
            DatabaseProvider db = new StandaloneDatabaseProvider(context);
            cache = new SimpleCache(
                    dir,
                    new LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES),
                    db
            );

            Log.d(TAG, "Cache created (limit="
                    + (MAX_CACHE_BYTES / (1024 * 1024)) + "MB)");
        }

        long used = cache.getCacheSpace();
        if (used >= MAX_CACHE_BYTES) {
            Log.w(TAG, "LRU eviction active (used="
                    + (used / (1024 * 1024)) + "MB)");
        }

        CacheKeyFactory keyFactory = spec ->
                fileId + ":" + spec.position;

        return new CacheDataSource.Factory()
                .setCache(cache)
                .setCacheKeyFactory(keyFactory)
                .setUpstreamDataSourceFactory(upstream)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
    }

}
