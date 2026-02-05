package com.github.jaykkumar01.vaultspace.media.controller;

import android.content.Context;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.media.helper.VideoMediaDriveHelper;
import java.io.File;
import java.util.Map;
@OptIn(markerClass = UnstableApi.class)
public final class VideoMediaController {

    private final PlayerView view;
    private final VideoMediaDriveHelper driveHelper;
    private ExoPlayer player;

    public VideoMediaController(@NonNull AppCompatActivity activity,
                                @NonNull PlayerView playerView) {
        this.view = playerView;
        this.driveHelper = new VideoMediaDriveHelper(activity);
        this.view.setVisibility(View.GONE);
    }

    public void show(@NonNull AlbumMedia media) {
        view.setVisibility(View.VISIBLE);

        driveHelper.resolve(media, new VideoMediaDriveHelper.Callback() {
            @Override
            public void onReady(@NonNull String url, @NonNull String token) {

                releasePlayer();

                // ---------- HTTP (alt=media + token) ----------
                DefaultHttpDataSource.Factory httpFactory =
                        new DefaultHttpDataSource.Factory()
                                .setAllowCrossProtocolRedirects(true)
                                .setDefaultRequestProperties(
                                        Map.of(
                                                "Authorization", "Bearer " + token,
                                                "Accept-Encoding", "identity"
                                        )
                                );

                // ---------- CACHE (read-through) ----------
                CacheDataSource.Factory cacheFactory =
                        new CacheDataSource.Factory()
                                .setCache(MediaCache.get(view.getContext()))
                                .setUpstreamDataSourceFactory(httpFactory)
                                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);

                // ---------- PLAYER (network build) ----------
                player = new ExoPlayer.Builder(view.getContext())
                        .setMediaSourceFactory(new DefaultMediaSourceFactory(cacheFactory))
                        .build();

                view.setPlayer(player);

                // ---------- LOOP ----------
                player.setRepeatMode(Player.REPEAT_MODE_ONE);

                // ---------- PLAY ----------
                player.setMediaItem(MediaItem.fromUri(url));
                player.prepare();
                player.play();
            }

            @Override
            public void onError(@NonNull Exception e) {
                // log / UI if needed
            }
        });
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    public void release() {
        releasePlayer();
        driveHelper.release();
    }

    /* ============================================================
     * Inner cache (single instance, LRU, lightweight)
     * ============================================================ */
    private static final class MediaCache {
        private static SimpleCache cache;

        static synchronized SimpleCache get(Context c) {
            if (cache == null) {
                File dir = new File(c.getCacheDir(), "exo_media_cache");
                cache = new SimpleCache(
                        dir,
                        new LeastRecentlyUsedCacheEvictor(150L * 1024 * 1024) // 150MB
                );
            }
            return cache;
        }
    }
}
