package com.github.jaykkumar01.vaultspace.media.controller;

import android.os.SystemClock;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.*;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.media.base.MediaLoadCallback;
import com.github.jaykkumar01.vaultspace.media.helper.VideoMediaDriveHelper;
import com.github.jaykkumar01.vaultspace.media.proxy.FastStartProxyController;

@UnstableApi
public final class VideoMediaController {

    private static final String TAG = "Proxy:VideoMediaController";

    private final PlayerView view;
    private final VideoMediaDriveHelper driveHelper;

    private ExoPlayer player;
    private FastStartProxyController proxy;

    private AlbumMedia media;
    private MediaLoadCallback callback;

    private long tPrepare;
    private boolean playWhenReady = true;

    public VideoMediaController(@NonNull AppCompatActivity activity,
                                @NonNull PlayerView playerView) {
        view = playerView;
        driveHelper = new VideoMediaDriveHelper(activity);
        view.setVisibility(View.GONE);
    }

    /* ======================= Public ======================= */

    public void setCallback(MediaLoadCallback cb) {
        callback = cb;
    }

    public void show(@NonNull AlbumMedia m) {
        media = m;
        view.setVisibility(View.GONE);
    }

    public void onStart() {
        if (player == null && media != null) prepare();
    }

    public void onPause() {
        if (player != null) {
            playWhenReady = player.getPlayWhenReady();
            player.pause();
        }
    }

    public void onResume() {
        if (player != null) {
            player.setPlayWhenReady(playWhenReady);
        }
    }

    public void release() {
        if (player != null) {
            player.release();
            player = null;
        }
        if (proxy != null) {
            proxy.stop();
            proxy = null;
        }
        driveHelper.release();
    }

    /* ======================= Core ======================= */

    private void prepare() {
        tPrepare = SystemClock.elapsedRealtime();
        if (callback != null) callback.onMediaLoading();

        driveHelper.resolve(media, new VideoMediaDriveHelper.Callback() {

            @Override
            public void onReady(@NonNull String url,
                                @NonNull String token) {

                proxy = new FastStartProxyController(
                        url, token, media.sizeBytes
                );

                proxy.start(localUrl -> startPlayer(localUrl));
            }

            @Override
            public void onError(@NonNull Exception e) {
                if (callback != null) callback.onMediaError(e);
            }
        });
    }

    /* ======================= Player ======================= */

    private void startPlayer(String url) {
        DefaultHttpDataSource.Factory http =
                new DefaultHttpDataSource.Factory();

        player = new ExoPlayer.Builder(view.getContext())
                .setMediaSourceFactory(new DefaultMediaSourceFactory(http))
                .build();

        player.setPlayWhenReady(true);
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        player.addListener(playerListener());

        view.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(url));
        player.prepare();
    }

    private Player.Listener playerListener() {
        return new Player.Listener() {

            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    long readyMs =
                            SystemClock.elapsedRealtime() - tPrepare;
                    long sizeMb =
                            media.sizeBytes / (1024 * 1024);

                    Log.d(TAG,
                            "READY in " + readyMs + " ms | " +
                                    sizeMb + " MB");

                    view.setVisibility(View.VISIBLE);
                    if (callback != null) callback.onMediaReady();
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                if (callback != null) callback.onMediaError(error);
            }
        };
    }
}
