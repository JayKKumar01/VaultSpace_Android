package com.github.jaykkumar01.vaultspace.media.controller;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.media.base.MediaLoadCallback;
import com.github.jaykkumar01.vaultspace.media.datasource.HybridDriveDataSource;

public final class VideoMediaController {

    private static final String TAG = "Video:MediaController";

    private final Context context;
    private final PlayerView view;
    private final Handler mainHandler;

    private ExoPlayer player;
    private AlbumMedia media;
    private MediaLoadCallback callback;

    private boolean playWhenReady = true;
    private long resumePosition = 0L;

    public VideoMediaController(@NonNull Context context, @NonNull PlayerView playerView) {
        this.context = context.getApplicationContext();
        this.view = playerView;
        this.view.setVisibility(View.GONE);
        this.mainHandler = new Handler(Looper.getMainLooper());
        Log.d(TAG, "Controller created");
    }

    /* ---------------- public api ---------------- */

    public void setCallback(MediaLoadCallback callback) {
        this.callback = callback;
    }

    public void show(@NonNull AlbumMedia media) {
        Log.d(TAG, "show(file=" + media.fileId + ")");
        if (this.media == null || !this.media.fileId.equals(media.fileId))
            resumePosition = 0L;
        this.media = media;
        view.setVisibility(View.GONE);
    }

    /* ---------------- lifecycle ---------------- */

    public void onStart() {
        Log.d(TAG, "onStart()");
        if (player == null && media != null) prepare();
    }

    public void onResume() {
        Log.d(TAG, "onResume()");
        if (player != null) player.setPlayWhenReady(playWhenReady);
    }

    public void onPause() {
        Log.d(TAG, "onPause()");
        releasePlayer();
    }

    public void onStop() {
        Log.d(TAG, "onStop()");
        releasePlayer();
    }

    public void release() {
        Log.d(TAG, "release()");
        releasePlayer();
        callback = null;
        media = null;
    }

    /* ---------------- prepare ---------------- */

    @OptIn(markerClass = UnstableApi.class)
    private void prepare() {
        Log.d(TAG, "prepare() start");
        view.setVisibility(View.GONE);
        if (callback != null) callback.onMediaLoading("Loading video…");

        DefaultMediaSourceFactory factory =
                new DefaultMediaSourceFactory(() -> {
                    Log.d(TAG, "DataSource created");
                    return new HybridDriveDataSource(context, media.fileId);
                });

        MediaItem item = MediaItem.fromUri("drive://" + media.fileId);

        player = new ExoPlayer.Builder(view.getContext())
                .setMediaSourceFactory(factory)
                .build();

        view.setPlayer(player);
//        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        player.setPlayWhenReady(playWhenReady);
        player.addListener(playerListener());

        player.setMediaItem(item);
        player.prepare();

        if (resumePosition > 0) player.seekTo(resumePosition);
    }

    /* ---------------- release ---------------- */

    private void releasePlayer() {
        mainHandler.post(() -> {
            if (player == null) return;

            Log.d(TAG, "releasePlayer @" + player.getCurrentPosition());

            resumePosition = player.getCurrentPosition();
            playWhenReady = player.getPlayWhenReady();

            player.release();
            player = null;
        });
    }

    /* ---------------- listener ---------------- */

    private Player.Listener playerListener() {
        return new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                Log.d(TAG, "player state = " + stateToString(state));
                if (state == Player.STATE_READY && player != null) {
                    mainHandler.post(() -> {
                        view.setVisibility(View.VISIBLE);
                        if (callback != null) callback.onMediaReady();
                    });
                }
            }
        };
    }

    private static String stateToString(int state) {
        return switch (state) {
            case Player.STATE_IDLE -> "IDLE";
            case Player.STATE_BUFFERING -> "BUFFERING";
            case Player.STATE_READY -> "READY";
            case Player.STATE_ENDED -> "ENDED";
            default -> "UNKNOWN(" + state + ")";
        };
    }
}
