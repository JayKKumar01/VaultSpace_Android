package com.github.jaykkumar01.vaultspace.media.controller;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.media.base.MediaLoadCallback;
import com.github.jaykkumar01.vaultspace.media.helper.VideoMediaDriveHelper;

@UnstableApi
public final class VideoMediaController {

    private final PlayerView view;
    private final VideoMediaDriveHelper driveHelper;

    private ExoPlayer player;
    private AlbumMedia media;
    private MediaLoadCallback callback;

    private boolean playWhenReady = true;
    private long resumePosition = 0L;

    public VideoMediaController(
            @NonNull AppCompatActivity activity,
            @NonNull PlayerView playerView) {
        this.view = playerView;
        this.driveHelper = new VideoMediaDriveHelper(activity);
        view.setVisibility(View.GONE);
    }

    public void setCallback(MediaLoadCallback callback) {
        this.callback = callback;
    }

    public void show(@NonNull AlbumMedia media) {
        this.media = media;
        view.setVisibility(View.GONE);
    }

    /* ---------------- lifecycle ---------------- */

    public void onStart() {
        if (player == null && media != null) prepare();
    }

    public void onResume() {
        if (player != null) player.setPlayWhenReady(playWhenReady);
    }

    public void onPause() {
        releasePlayerInternal();
    }

    public void onStop() {
        releasePlayerInternal();
    }

    public void release() {
        releasePlayerInternal();
        driveHelper.release();
        callback = null;
        media = null;
    }

    /* ---------------- player ---------------- */

    private void prepare() {
        view.setVisibility(View.GONE);
        notifyLoading("Getting things ready…");

        driveHelper.prepare(media, new VideoMediaDriveHelper.Callback() {
            @Override
            public void onReady(
                    @NonNull DefaultMediaSourceFactory factory,
                    @NonNull String url) {

                player = new ExoPlayer.Builder(view.getContext())
                        .setMediaSourceFactory(factory)
                        .build();

                player.setRepeatMode(Player.REPEAT_MODE_ONE);
                player.setPlayWhenReady(playWhenReady);
                player.addListener(playerListener());

                view.setPlayer(player);
                player.setMediaItem(MediaItem.fromUri(url));
                player.prepare();

                if (resumePosition > 0) player.seekTo(resumePosition);
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
                if (state == Player.STATE_BUFFERING)
                    notifyLoading("Loading video…");

                if (state == Player.STATE_READY && player != null) {
                    view.setVisibility(View.VISIBLE);
                    if (callback != null) callback.onMediaReady();
                }
            }
        };
    }

    private void releasePlayerInternal() {
        if (player == null) return;
        resumePosition = player.getCurrentPosition();
        playWhenReady = player.getPlayWhenReady();
        player.release();
        player = null;
    }

    private void notifyLoading(String text) {
        if (callback != null) callback.onMediaLoading(text);
    }
}
