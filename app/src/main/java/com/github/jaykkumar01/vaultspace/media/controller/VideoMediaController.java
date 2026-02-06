package com.github.jaykkumar01.vaultspace.media.controller;

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

    private static final long CACHE_LIMIT_BYTES = 100L * 1024 * 1024;

    private final PlayerView view;
    private final VideoMediaDriveHelper driveHelper;
    private final File cacheDir;
    private final StandaloneDatabaseProvider databaseProvider;

    private SimpleCache cache;
    private ExoPlayer player;
    private AlbumMedia media;
    private MediaLoadCallback callback;
    private boolean playWhenReady = true;

    public VideoMediaController(@NonNull AppCompatActivity activity,
                                @NonNull PlayerView playerView) {
        this.view = playerView;
        this.driveHelper = new VideoMediaDriveHelper(activity);
        this.cacheDir = new File(activity.getCacheDir(), "video_tmp");
        this.databaseProvider = new StandaloneDatabaseProvider(activity);
        this.view.setVisibility(View.GONE);
    }

    public void setCallback(MediaLoadCallback callback) {
        this.callback = callback;
    }

    public void show(@NonNull AlbumMedia media) {
        this.media = media;
        if (player == null) prepare();
        else view.setVisibility(View.VISIBLE);
    }

    public void onStart() {
        if (player == null && media != null) prepare();
    }

    public void onResume() {
        if (player != null) player.setPlayWhenReady(playWhenReady);
    }

    public void onPause() {
        if (player != null) {
            playWhenReady = player.getPlayWhenReady();
            player.pause();
        }
    }

    public void onStop() {
    }

    public void release() {
        releasePlayer();
        releaseCache();
        driveHelper.release();
        callback = null;
        media = null;
    }

    private void prepare() {
        if (media == null) return;
        view.setVisibility(View.GONE);
        if (callback != null) callback.onMediaLoading();

        driveHelper.resolve(media, new VideoMediaDriveHelper.Callback() {

            @Override
            public void onReady(@NonNull String url,
                                @NonNull String token) {

                releasePlayer();

                DefaultHttpDataSource.Factory http =
                        new DefaultHttpDataSource.Factory()
                                .setAllowCrossProtocolRedirects(true)
                                .setDefaultRequestProperties(buildHeaders(token));

                DefaultMediaSourceFactory mediaSourceFactory;

                if (media.sizeBytes <= CACHE_LIMIT_BYTES) {
                    initCache();
                    CacheDataSource.Factory cacheFactory =
                            new CacheDataSource.Factory()
                                    .setCache(cache)
                                    .setUpstreamDataSourceFactory(http)
                                    .setCacheReadDataSourceFactory(new FileDataSource.Factory())
                                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
                    mediaSourceFactory = new DefaultMediaSourceFactory(cacheFactory);
                } else {
                    mediaSourceFactory = new DefaultMediaSourceFactory(http);
                }

                player = new ExoPlayer.Builder(view.getContext())
                        .setMediaSourceFactory(mediaSourceFactory)
                        .build();

                player.setRepeatMode(Player.REPEAT_MODE_ONE);
                player.setPlayWhenReady(playWhenReady);
                player.addListener(playerListener());

                view.setPlayer(player);
                player.setMediaItem(MediaItem.fromUri(url));
                player.prepare();
            }

            @Override
            public void onError(@NonNull Exception e) {
                if (callback != null) callback.onMediaError(e);
            }
        });
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void initCache() {
        if (cache != null) return;
        if (!cacheDir.exists()) cacheDir.mkdirs();
        cache = new SimpleCache(
                cacheDir,
                new LeastRecentlyUsedCacheEvictor(Long.MAX_VALUE),
                databaseProvider
        );
    }

    private Player.Listener playerListener() {
        return new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    view.setVisibility(View.VISIBLE);
                    if (callback != null) callback.onMediaReady();
                    if (player != null) player.removeListener(this);
                }
            }
        };
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private void releaseCache() {
        if (cache != null) {
            try {
                cache.release();
            } catch (Exception ignored) {}
            deleteDir(cacheDir);
            cache = null;
        }
    }

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
