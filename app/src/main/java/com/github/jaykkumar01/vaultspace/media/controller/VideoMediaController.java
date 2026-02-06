package com.github.jaykkumar01.vaultspace.media.controller;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.media.base.MediaLoadCallback;
import com.github.jaykkumar01.vaultspace.media.helper.VideoMediaDriveHelper;

import java.util.HashMap;
import java.util.Map;

public final class VideoMediaController {

    private final PlayerView view;
    private final VideoMediaDriveHelper driveHelper;
    private ExoPlayer player;
    private AlbumMedia media;
    private MediaLoadCallback callback;
    private boolean playWhenReady = true;

    public VideoMediaController(@NonNull AppCompatActivity activity,
                                @NonNull PlayerView playerView) {
        this.view = playerView;
        this.driveHelper = new VideoMediaDriveHelper(activity);
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
        driveHelper.release();
        callback = null;
        media = null;
    }

    private void prepare() {
        if (media == null) return;
        view.setVisibility(View.GONE);
        if (callback != null) callback.onMediaLoading();

        driveHelper.resolve(media, new VideoMediaDriveHelper.Callback() {

            @OptIn(markerClass = UnstableApi.class)
            @Override
            public void onReady(@NonNull String url,
                                @NonNull String token) {

                releasePlayer();

                DefaultHttpDataSource.Factory httpFactory =
                        new DefaultHttpDataSource.Factory()
                                .setAllowCrossProtocolRedirects(true)
                                .setDefaultRequestProperties(buildHeaders(token));

                player = new ExoPlayer.Builder(view.getContext())
                        .setMediaSourceFactory(new DefaultMediaSourceFactory(httpFactory))
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
