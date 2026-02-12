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
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.ui.PlayerView;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.media.base.MediaLoadCallback;
import com.github.jaykkumar01.vaultspace.media.cache.DriveAltMediaCache;
import com.github.jaykkumar01.vaultspace.media.datasource.DriveDataSource;

@UnstableApi
public final class VideoMediaController {

    /* ---------------- CONSTANTS ---------------- */

    private static final String TAG = "Video:MediaController";

    /* ---------------- CORE ---------------- */

    private final Context context;
    private final PlayerView view;
    private final Handler main;
    private final DriveAltMediaCache cache;

    private ExoPlayer player;
    private AlbumMedia media;
    private MediaLoadCallback callback;

    private boolean playWhenReady = true;
    private long resumePosition = 0L;
    private DriveDataSource driveSource;

    /* ---------------- CONSTRUCTOR ---------------- */

    public VideoMediaController(@NonNull Context context, @NonNull PlayerView view) {
        this.context = context.getApplicationContext();
        this.view = view;
        this.cache = new DriveAltMediaCache(context);
        this.main = new Handler(Looper.getMainLooper());
        view.setVisibility(GONE);
        Log.d(TAG, "created");
    }

    /* ---------------- PUBLIC API ---------------- */

    public void setCallback(MediaLoadCallback callback) {
        this.callback = callback;
    }

    public void show(@NonNull AlbumMedia media) {
        Log.d(TAG, "show(" + media.fileId + ")");
        if (this.media == null || !this.media.fileId.equals(media.fileId)) resumePosition = 0L;
        this.media = media;
        driveSource = new DriveDataSource(context, media);
        
        view.setVisibility(GONE);
    }

    public void onStart() {
        if (player == null && media != null) preparePlayer();
    }

    public void onResume() {
        if (player != null) player.setPlayWhenReady(playWhenReady);
    }

    public void onPause() {
        releasePlayer();
    }

    public void onStop() {
        releasePlayer();
    }

    public void release() {
        cache.release();
        releasePlayer();
        callback = null;
        media = null;
    }

    /* ---------------- PREPARE PLAYER ---------------- */

    @OptIn(markerClass = UnstableApi.class)
    private void preparePlayer() {

        Log.d(TAG, "preparePlayer()");
        view.setVisibility(GONE);

        if (callback != null)
            callback.onMediaLoading("Loading video…");

        /* ---------- datasource ---------- */

        DataSource.Factory upstream = () -> driveSource;
        CacheDataSource.Factory cacheFactory = cache.wrap(media.fileId, upstream);

        /* ---------- media source (EXPLICIT) ---------- */

        MediaItem mediaItem = MediaItem.fromUri("vaultspace://drive/" + media.fileId);


        ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(cacheFactory).createMediaSource(mediaItem);

        /* ---------- buffering ---------- */

        DefaultLoadControl loadControl = new DefaultLoadControl.Builder().setBufferDurationsMs(
                        20_000,
                        30_000,
                        1_000,
                        2_000
                )
                .build();

        /* ---------- player ---------- */

        player = new ExoPlayer.Builder(view.getContext())
                .setLoadControl(loadControl)
                .build();

        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        player.setPlayWhenReady(playWhenReady);
        player.addListener(playerListener());

        /* ---------- attach ---------- */

        player.setMediaSource(mediaSource);
        player.prepare();

        if (resumePosition > 0)
            player.seekTo(resumePosition);
    }


    /* ---------------- RELEASE PLAYER ---------------- */

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

    /* ---------------- PLAYER LISTENER ---------------- */

    private Player.Listener playerListener() {
        return new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                Log.d(TAG, "state=" + stateToString(state));
                if (state == Player.STATE_READY && player != null) {
                    main.post(() -> {
                        if (view.getPlayer() == null) view.setPlayer(player);
                        view.setVisibility(View.VISIBLE);
                        if (callback != null) callback.onMediaReady();
                    });
                }
            }
        };
    }

    /* ---------------- STATE STRING ---------------- */

    private static String stateToString(int s) {
        return switch (s) {
            case Player.STATE_IDLE -> "IDLE";
            case Player.STATE_BUFFERING -> "BUFFERING";
            case Player.STATE_READY -> "READY";
            case Player.STATE_ENDED -> "ENDED";
            default -> "UNKNOWN(" + s + ")";
        };
    }
}
