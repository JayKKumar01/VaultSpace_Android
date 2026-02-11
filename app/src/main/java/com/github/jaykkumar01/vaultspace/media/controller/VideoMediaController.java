package com.github.jaykkumar01.vaultspace.media.controller;

import static android.view.View.GONE;

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
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.media.base.MediaLoadCallback;
import com.github.jaykkumar01.vaultspace.media.datasource.DriveDataSource;

public final class VideoMediaController {

    private static final String TAG = "Video:MediaController";

    /* ---------------- core ---------------- */

    private final Context context;
    private final PlayerView view;
    private final Handler main;

    private ExoPlayer player;
    private AlbumMedia media;
    private MediaLoadCallback callback;

    private boolean playWhenReady = true;
    private long resumePosition = 0L;

    /* ---------------- lifecycle ---------------- */

    public VideoMediaController(@NonNull Context context,@NonNull PlayerView view) {
        this.context = context.getApplicationContext();
        this.view = view;
        this.main = new Handler(Looper.getMainLooper());
        view.setVisibility(GONE);
        Log.d(TAG,"created");
    }

    /* ---------------- public api ---------------- */

    public void setCallback(MediaLoadCallback callback) {
        this.callback = callback;
    }

    public void show(@NonNull AlbumMedia media) {
        Log.d(TAG,"show("+media.fileId+")");
        if (this.media == null || !this.media.fileId.equals(media.fileId))
            resumePosition = 0L;
        this.media = media;
        view.setVisibility(GONE);
    }

    public void onStart() {
        if (player == null && media != null) preparePlayer();
    }

    public void onResume() {
        if (player != null) player.setPlayWhenReady(playWhenReady);
    }

    public void onPause() { releasePlayer(); }

    public void onStop() { releasePlayer(); }

    public void release() {
        releasePlayer();
        callback = null;
        media = null;
    }

    /* ---------------- prepare ---------------- */

    @OptIn(markerClass = UnstableApi.class)
    private void preparePlayer() {
        Log.d(TAG,"preparePlayer()");
        view.setVisibility(View.GONE);
        if (callback != null) callback.onMediaLoading("Loading video…");

        DataSource.Factory factory = () ->
                new DriveDataSource(
                        context,
                        media.fileId,
                        media.sizeBytes
                );

        DefaultMediaSourceFactory msf = new DefaultMediaSourceFactory(factory);

        MediaItem item = MediaItem.fromUri("vaultspace://" + media.fileId);

        player = new ExoPlayer.Builder(view.getContext())
                .setMediaSourceFactory(msf)
                .build();

        player.setPlayWhenReady(playWhenReady);
        player.addListener(playerListener());
        player.setMediaItem(item);
        player.prepare();

        if (resumePosition > 0) player.seekTo(resumePosition);
    }

    /* ---------------- release ---------------- */

    private void releasePlayer() {
        main.post(() -> {
            if (player == null) return;
            resumePosition = player.getCurrentPosition();
            playWhenReady = player.getPlayWhenReady();
            if (view.getPlayer() != null) view.setPlayer(null);
            player.release();
            player = null;
        });
    }

    /* ---------------- listener ---------------- */

    private Player.Listener playerListener() {
        return new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                Log.d(TAG,"state="+stateToString(state));
                if (state == Player.STATE_READY && player != null) {
                    main.post(() -> {
                        if (view.getPlayer() == null)
                            view.setPlayer(player);
                        view.setVisibility(View.VISIBLE);
                        if (callback != null) callback.onMediaReady();
                    });
                }
            }
        };
    }

    private static String stateToString(int s) {
        return switch (s) {
            case Player.STATE_IDLE -> "IDLE";
            case Player.STATE_BUFFERING -> "BUFFERING";
            case Player.STATE_READY -> "READY";
            case Player.STATE_ENDED -> "ENDED";
            default -> "UNKNOWN("+s+")";
        };
    }
}
