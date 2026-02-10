package com.github.jaykkumar01.vaultspace.media.helper;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.CacheWriter;

@UnstableApi
public final class DriveCacheWarmUpHelper {

    private static final String TAG = "DriveCacheWarmUp";
    private static final long LOG_STEP_PERCENT = 5;

    private DriveCacheWarmUpHelper() {}

    /**
     * Warm up cache using Exo HTTP pipeline.
     * MUST be called on background thread.
     */
    public static void warmUp(
            @NonNull String fileId,
            @NonNull CacheDataSource.Factory cacheFactory,
            long sizeBytes
    ) throws Exception {

        Log.d(TAG, "[" + fileId + "] warm-up start (" + sizeBytes + " bytes)");

        Uri uri = Uri.parse(
                "https://www.googleapis.com/drive/v3/files/"
                        + fileId + "?alt=media"
        );

        DataSpec spec = new DataSpec(uri, 0, sizeBytes);

        CacheDataSource cacheDataSource = cacheFactory.createDataSource();

        CacheWriter.ProgressListener progress =
                new CacheWriter.ProgressListener() {

                    long lastBucket = -1;

                    @Override
                    public void onProgress(
                            long requestLength,
                            long bytesCached,
                            long newBytesCached
                    ) {
                        if (requestLength <= 0) return;

                        long percent = (bytesCached * 100) / requestLength;
                        long bucket = percent / LOG_STEP_PERCENT;

                        if (bucket != lastBucket) {
                            lastBucket = bucket;
                            Log.d(
                                    TAG,
                                    "[" + fileId + "] cached "
                                            + percent + "% ("
                                            + bytesCached + "/"
                                            + requestLength + ")"
                            );
                        }
                    }
                };

        CacheWriter writer = new CacheWriter(
                cacheDataSource,
                spec,
                null,        // internal buffer
                progress
        );

        writer.cache(); // ✅ correct API

        Log.d(TAG, "[" + fileId + "] warm-up complete");
    }
}
