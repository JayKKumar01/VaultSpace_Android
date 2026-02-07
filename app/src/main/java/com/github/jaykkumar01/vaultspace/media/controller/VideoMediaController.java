package com.github.jaykkumar01.vaultspace.media.controller;

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
import com.github.jaykkumar01.vaultspace.media.proxy.ProgressiveProxyController;

@UnstableApi
public final class VideoMediaController {

    private static final String TAG = "Proxy:VideoMediaController";

    private final PlayerView view;
    private final VideoMediaDriveHelper driveHelper;

    private ExoPlayer player;
    private ProgressiveProxyController proxy;
    private AlbumMedia media;
    private MediaLoadCallback callback;

    private boolean playWhenReady = true;

    public VideoMediaController(@NonNull AppCompatActivity a,
                                @NonNull PlayerView v) {
        Log.d(TAG, "ctor()");
        view = v;
        driveHelper = new VideoMediaDriveHelper(a);
        view.setVisibility(View.GONE);
    }

    public void setCallback(MediaLoadCallback cb) {
        callback = cb;
    }

    public void show(@NonNull AlbumMedia m) {
        Log.d(TAG, "show(): " + m.name);
        media = m;
        view.setVisibility(View.GONE);
    }

    public void onStart() {
        if (player == null && media != null) prepare();
    }

    private void prepare() {
        Log.d(TAG, "prepare()");
        if (callback != null) callback.onMediaLoading();

        driveHelper.resolve(media, new VideoMediaDriveHelper.Callback() {
            @Override
            public void onReady(@NonNull String url, @NonNull String token) {
                proxy = new ProgressiveProxyController(url, token, media.sizeBytes);
                proxy.start();
                startPlayer(proxy.videoUrl());
            }

            @Override
            public void onError(@NonNull Exception e) {
                if (callback != null) callback.onMediaError(e);
            }
        });
    }

    private void startPlayer(String url) {
        Log.d(TAG, "startPlayer(): " + url);

        DefaultHttpDataSource.Factory http =
                new DefaultHttpDataSource.Factory()
                        .setAllowCrossProtocolRedirects(false);

        player = new ExoPlayer.Builder(view.getContext())
                .setMediaSourceFactory(new DefaultMediaSourceFactory(http))
                .build();

        player.setPlayWhenReady(playWhenReady);
        player.addListener(listener());

        view.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(url));
        player.prepare();
    }

    private Player.Listener listener() {
        return new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    Log.d(TAG, "Player READY");
                    view.setVisibility(View.VISIBLE);
                    if (callback != null) callback.onMediaReady();
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Log.e(TAG, "Player error", error);
                if (callback != null) callback.onMediaError(error);
            }
        };
    }

    public void release() {
        Log.d(TAG, "release()");
        if (player != null) player.release();
        if (proxy != null) proxy.stop();
        driveHelper.release();
    }
}
