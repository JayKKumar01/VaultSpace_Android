package com.github.jaykkumar01.vaultspace.media.cache;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.database.DatabaseProvider;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.datasource.cache.Cache;
import androidx.media3.datasource.cache.CacheDataSink;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;

import com.github.jaykkumar01.vaultspace.core.auth.DriveAuthGate1;

import java.io.File;
import java.util.Map;

@UnstableApi
public final class DriveSingleFileCacheHelper {

    private static final String TAG = "DriveSingleFileCache";
    private static final long MAX_CACHE_BYTES = 512L * 1024L * 1024L;

    private static SimpleCache cache;

    private DriveSingleFileCacheHelper() {}

    /* ---------------- cache ---------------- */

    private static synchronized Cache getCache(Context context) {
        if (cache == null) {
            File dir = new File(context.getCacheDir(), "exo_drive_single_file_cache");
            DatabaseProvider db = new StandaloneDatabaseProvider(context);
            cache = new SimpleCache(
                    dir,
                    new LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES),
                    db
            );
        }
        return cache;
    }

    /* ---------------- ExoPlayer wiring ---------------- */

    public static CacheDataSource.Factory wrap(
            @NonNull Context context,
            @NonNull String fileId,
            @NonNull DataSource.Factory upstream
    ) {
        return new CacheDataSource.Factory()
                .setCache(getCache(context.getApplicationContext()))
                .setCacheKeyFactory(spec -> fileId)
                .setUpstreamDataSourceFactory(upstream)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
    }

    /* ---------------- explicit warm-up ---------------- */

    public static void warmUpRange(
            @NonNull Context context,
            @NonNull String fileId,
            long position,
            int length
    ) throws Exception {

        Context app = context.getApplicationContext();
        String token = DriveAuthGate1.get(app).requireToken();

        Uri uri = Uri.parse(
                "https://www.googleapis.com/drive/v3/files/" + fileId + "?alt=media"
        );

        Log.d(TAG, "warmUp @" + position + " len=" + length);

        HttpDataSource source =
                new DefaultHttpDataSource.Factory()
                        .setDefaultRequestProperties(
                                Map.of(
                                        "Authorization", "Bearer " + token,
                                        "Range", "bytes=" + position + "-"
                                )
                        )
                        .createDataSource();

        CacheDataSink sink =
                new CacheDataSink(
                        getCache(app),
                        CacheDataSink.DEFAULT_FRAGMENT_SIZE
                );

        DataSpec spec = new DataSpec(uri, position, length);

        try {
            source.open(spec);
            sink.open(spec);

            byte[] buf = new byte[16 * 1024];
            int remaining = length;

            while (remaining > 0) {
                int r = source.read(buf, 0, Math.min(buf.length, remaining));
                if (r == -1) break;
                sink.write(buf, 0, r);
                remaining -= r;
            }
        } finally {
            try { source.close(); } catch (Exception ignored) {}
            try { sink.close(); } catch (Exception ignored) {}
        }
    }
}
