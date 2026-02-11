package com.github.jaykkumar01.vaultspace.media.cache;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.cache.Cache;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;

import com.github.jaykkumar01.vaultspace.core.auth.DriveAuthGate;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@UnstableApi
public final class DriveAltMediaCache {

    /* ---------------- CONSTANTS ---------------- */

    private static final String TAG = "DriveAltMediaCache";
    private static final long MAX_CACHE_BYTES = 120L * 1024L * 1024L;
    private static final String BASE_URL = "https://www.googleapis.com/drive/v3/files/";
    private static final int WARM_RANGE_BYTES = 36;

    /* ---------------- CACHE ---------------- */

    private static SimpleCache cache;

    /* ---------------- WARM EXECUTOR (3 THREADS) ---------------- */

    private static final ExecutorService warmExecutor =
            Executors.newFixedThreadPool(3, r -> {
                Thread t = new Thread(r, "Drive-AltMedia-Warm");
                t.setDaemon(true);
                return t;
            });

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

    /* ---------------- PARALLEL WARMUP (3 BLASTS) ---------------- */

    public void warmAltMedia(@NonNull String fileId) {

        for (int i = 1; i <= 3; i++) {
            final int index = i;
            warmExecutor.execute(() -> warmRequest(fileId, index));
        }
    }

    private void warmRequest(String fileId, int index) {

        HttpURLConnection conn = null;
        try {
            String token = DriveAuthGate.get(context).getToken();

            URL url = new URL(BASE_URL + fileId + "?alt=media");
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Accept-Encoding", "identity");
            conn.setRequestProperty("Range", "bytes=0-" + WARM_RANGE_BYTES);

            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setUseCaches(false);

            conn.connect();

            int code = conn.getResponseCode();
            if (code == 200 || code == 206) {
                InputStream in = conn.getInputStream();
                byte[] tmp = new byte[64];
                in.read(tmp);
                in.close();
                Log.d(TAG, "WARM#" + index + " success " + fileId);
            } else {
                Log.d(TAG, "WARM#" + index + " http=" + code);
            }

        } catch (Exception e) {
            Log.d(TAG, "WARM#" + index + " fail " + e.getClass().getSimpleName());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /* ---------------- RELEASE ---------------- */

    public void release() {
        // executor intentionally persistent
        // cache intentionally app-lifetime
    }
}
