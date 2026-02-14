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
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.ui.PlayerView;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.media.base.MediaLoadCallback;
import com.github.jaykkumar01.vaultspace.media.cache.DriveAltMediaCache;
import com.github.jaykkumar01.vaultspace.media.datasource.DriveDataSource;

@UnstableApi
public final class VideoMediaController {

    private static final String TAG = "Video:MediaController";
    private static final String SCHEME = "vaultspace://drive/";

    /* ---------------- CORE ---------------- */

    private final Context context;
    private final PlayerView view;
    private final MediaLoadCallback callback;
    private final DriveAltMediaCache cache;

    private final Handler main = new Handler(Looper.getMainLooper());

    private ExoPlayer player;
    private AlbumMedia media;

    private boolean playWhenReady = true;
    private long resumePosition = 0L;

    /* ---------------- DATA PIPELINE ---------------- */

    private DriveDataSource driveSource;
    private CacheDataSource.Factory cacheFactory;

    /* ---------------- CONSTRUCTOR ---------------- */

    public VideoMediaController(@NonNull Context context,
                                @NonNull PlayerView view,
                                @NonNull MediaLoadCallback callback) {
        this.context = context.getApplicationContext();
        this.view = view;
        this.callback = callback;
        this.cache = new DriveAltMediaCache(this.context);
        view.setVisibility(GONE);
        Log.d(TAG, "created");
    }

    /* ============================================================= */
    /* ======================== PUBLIC API ========================== */
    /* ============================================================= */

    public void show(@NonNull AlbumMedia media) {

        Log.d(TAG, "show(" + media.fileId + ")");
        this.media = media;

        /* ---- Playback DataSource ---- */

        driveSource = new DriveDataSource(context, media);
        DataSource.Factory playbackUpstream = () -> driveSource;
        cacheFactory = cache.wrap(media.fileId, playbackUpstream);
        view.setVisibility(GONE);
    }


    public void onStart() {
        if (player != null || media == null) return;

        callback.onMediaLoading("Loading video…");
        preparePlayer();
    }

    public void onResume() {
        if (player != null)
            player.setPlayWhenReady(playWhenReady);
    }

    public void onPause() {
        releasePlayer();
    }

    public void onStop() {
        releasePlayer();
    }

    public void release() {
        releasePlayer();
        cache.release();
        media = null;
    }

    /* ============================================================= */
    /* ===================== PLAYER CREATION ======================= */
    /* ============================================================= */

    @OptIn(markerClass = UnstableApi.class)
    private void preparePlayer() {

        Log.d(TAG, "preparePlayer()");
        view.setVisibility(GONE);

        MediaItem mediaItem = MediaItem.fromUri(SCHEME + media.fileId);

        ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(cacheFactory)
                .createMediaSource(mediaItem);

        DefaultLoadControl loadControl = new DefaultLoadControl.Builder().setBufferDurationsMs(
                        5_000,   // min buffer
                        30_000,   // max buffer
                        1_000,    // playback start
                        2_000     // rebuffer
                )
                .build();

        player = new ExoPlayer.Builder(view.getContext()).setLoadControl(loadControl).build();

        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        player.setPlayWhenReady(playWhenReady);
        player.addListener(playerListener());

        player.setMediaSource(mediaSource);
        player.prepare();

        if (resumePosition > 0)
            player.seekTo(resumePosition);
    }

    /* ============================================================= */
    /* ===================== PLAYER RELEASE ======================== */
    /* ============================================================= */

    private void releasePlayer() {

        if (player == null) return;

        driveSource.onPlayerRelease();

        resumePosition = player.getCurrentPosition();
        playWhenReady = player.getPlayWhenReady();

        if (view.getPlayer() != null)
            view.setPlayer(null);

        player.release();
        player = null;
    }

    /* ============================================================= */
    /* ====================== PLAYER LISTENER ====================== */
    /* ============================================================= */

    private Player.Listener playerListener() {
        return new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {

                Log.d(TAG, "state=" + stateToString(state));

                if (state == Player.STATE_READY && player != null) {

                    driveSource.onPlayerReady();

                    main.post(() -> {
                        if (view.getPlayer() == null)
                            view.setPlayer(player);

                        view.setVisibility(View.VISIBLE);
                        callback.onMediaReady();
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
            default -> "UNKNOWN(" + s + ")";
        };
    }
}