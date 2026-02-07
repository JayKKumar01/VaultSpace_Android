package com.github.jaykkumar01.vaultspace.media.controller;

import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.FileDataSource;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.media.base.MediaLoadCallback;
import com.github.jaykkumar01.vaultspace.media.helper.VideoMediaDriveHelper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@UnstableApi
public final class VideoMediaController {

    private static final String TAG = "VaultSpace:VideoCtrl";
    private static final long CACHE_LIMIT_BYTES = 200L * 1024 * 1024;

    private final PlayerView view;
    private final VideoMediaDriveHelper driveHelper;
    private final StandaloneDatabaseProvider databaseProvider;

    private SimpleCache cache;
    private File cacheDir;
    private ExoPlayer player;

    private AlbumMedia media;
    private MediaLoadCallback callback;

    private boolean playWhenReady = true;
    private long resumePosition = 0L;

    public VideoMediaController(@NonNull AppCompatActivity activity,
                                @NonNull PlayerView playerView) {
        this.view = playerView;
        this.driveHelper = new VideoMediaDriveHelper(activity);
        this.databaseProvider = new StandaloneDatabaseProvider(activity);
        this.view.setVisibility(View.GONE);
        Log.d(TAG, "controller created");
    }

    public void setCallback(MediaLoadCallback callback) {
        this.callback = callback;
    }

    public void show(@NonNull AlbumMedia media) {
        this.media = media;
        view.setVisibility(View.GONE);
        Log.d(TAG, "show media id=" + media.fileId);
    }

    /* ============================== lifecycle ============================== */

    public void onStart() {
        Log.d(TAG, "onStart");
        if (player == null && media != null) prepare();
    }

    public void onResume() {
        Log.d(TAG, "onResume");
        if (player != null) player.setPlayWhenReady(playWhenReady);
    }

    public void onPause() {
        Log.d(TAG, "onPause");
        releasePlayerInternal();
    }

    public void onStop() {
        Log.d(TAG, "onStop");
        releasePlayerInternal();
    }

    public void release() {
        Log.d(TAG, "release (destroy)");
        releasePlayerInternal();
        releaseCache();
        driveHelper.release();
        callback = null;
        media = null;
    }

    /* ============================== prepare ============================== */

    private void prepare() {
        if (media == null) return;

        Log.d(TAG, "prepare player");
        view.setVisibility(View.GONE);
        if (callback != null) callback.onMediaLoading();

        driveHelper.resolve(media, new VideoMediaDriveHelper.Callback() {
            @Override
            public void onReady(@NonNull String url, @NonNull String token) {
                Log.d(TAG, "drive resolved, building player");

                DefaultMediaSourceFactory mediaSourceFactory =
                        buildMediaSourceFactory(view.getContext(), token);

                player = new ExoPlayer.Builder(view.getContext())
                        .setMediaSourceFactory(mediaSourceFactory)
                        .build();

                player.setRepeatMode(Player.REPEAT_MODE_ONE);
                player.setPlayWhenReady(playWhenReady);
                player.addListener(playerListener());

                view.setPlayer(player);
                player.setMediaItem(MediaItem.fromUri(url));
                player.prepare();

                if (resumePosition > 0) {
                    Log.d(TAG, "seek to " + resumePosition);
                    player.seekTo(resumePosition);
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "media resolve failed", e);
                if (callback != null) callback.onMediaError(e);
            }
        });
    }

    private DefaultMediaSourceFactory buildMediaSourceFactory(Context c, String token) {
        DefaultHttpDataSource.Factory http =
                new DefaultHttpDataSource.Factory()
                        .setAllowCrossProtocolRedirects(true)
                        .setDefaultRequestProperties(buildHeaders(token));

        if (media.sizeBytes > CACHE_LIMIT_BYTES) {
            Log.d(TAG, "cache skipped (size > limit)");
            return new DefaultMediaSourceFactory(http);
        }

        initCache(c);

        CacheDataSource.Factory cacheFactory =
                new CacheDataSource.Factory()
                        .setCache(cache)
                        .setUpstreamDataSourceFactory(http)
                        .setCacheReadDataSourceFactory(new FileDataSource.Factory())
                        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);

        return new DefaultMediaSourceFactory(cacheFactory);
    }

    /* ============================== cache ============================== */

    private void initCache(Context c) {
        if (cache != null) return;
        cacheDir = new File(c.getCacheDir(), "video_tmp_" + System.nanoTime());
        cacheDir.mkdirs();
        cache = new SimpleCache(
                cacheDir,
                new LeastRecentlyUsedCacheEvictor(Long.MAX_VALUE),
                databaseProvider
        );
        Log.d(TAG, "cache created: " + cacheDir.getName());
    }

    private void releaseCache() {
        if (cache == null) return;
        File oldDir = cacheDir;
        try { cache.release(); } catch (Exception ignored) {}
        cache = null;
        cacheDir = null;
        Log.d(TAG, "cache released");
        new Thread(() -> deleteDir(oldDir)).start();
    }

    /* ============================== player ============================== */

    private void releasePlayerInternal() {
        if (player == null) return;
        resumePosition = player.getCurrentPosition();
        playWhenReady = player.getPlayWhenReady();
        Log.d(TAG, "player released at " + resumePosition);
        player.release();
        player = null;
    }

    private Player.Listener playerListener() {
        return new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    Log.d(TAG, "player READY");
                    view.setVisibility(View.VISIBLE);
                    if (callback != null) callback.onMediaReady();
                    if (player != null) player.removeListener(this);
                }
            }
        };
    }

    /* ============================== utils ============================== */

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void deleteDir(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) {
            if (f.isDirectory()) deleteDir(f);
            else f.delete();
        }
        dir.delete();
    }

    private static Map<String, String> buildHeaders(String token) {
        Map<String, String> h = new HashMap<>();
        h.put("Authorization", "Bearer " + token);
        h.put("Accept-Encoding", "identity");
        h.put("Accept", "*/*");
        h.put("Connection", "keep-alive");
        h.put("Range", "bytes=0-");
        return h;
    }
}
