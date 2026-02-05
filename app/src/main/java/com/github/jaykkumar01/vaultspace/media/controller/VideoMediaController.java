package com.github.jaykkumar01.vaultspace.media.controller;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.media.helper.VideoMediaDriveHelper;

public final class VideoMediaController {

    private final PlayerView playerView;
    private final ExoPlayer player;
    private final VideoMediaDriveHelper driveHelper;

    public VideoMediaController(@NonNull AppCompatActivity activity,
                                @NonNull PlayerView playerView) {

        this.playerView = playerView;
        this.player = new ExoPlayer.Builder(activity).build();
        this.driveHelper = new VideoMediaDriveHelper(activity);

        this.playerView.setPlayer(player);
        this.playerView.setVisibility(View.GONE);
    }

    public void show(@NonNull AlbumMedia media) {
        playerView.setVisibility(View.VISIBLE);

        driveHelper.buildMediaSource(media, new VideoMediaDriveHelper.Callback() {
            @Override
            public void onReady(@NonNull androidx.media3.exoplayer.source.MediaSource source) {
                player.setMediaSource(source);
                player.prepare();
                player.play();
            }

            @Override
            public void onError(@NonNull Exception e) {
                // TODO: error UI / logging
            }
        });
    }

    public void release() {
        player.release();
        driveHelper.release();
    }
}
