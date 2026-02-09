package com.github.jaykkumar01.vaultspace.media.controller;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.media.base.MediaLoadCallback;

import java.util.concurrent.atomic.AtomicBoolean;

public final class VideoMediaController {

    private final Context context;
    /* ---------------- ui ---------------- */
    private final PlayerView view;

    /* ---------------- playback ---------------- */

    private ExoPlayer player;
    private AlbumMedia media;

    /* ---------------- callbacks ---------------- */

    private MediaLoadCallback callback;

    /* ---------------- orchestration ---------------- */

    private VideoMediaSession session;


    /* ---------------- state ---------------- */

    private boolean playWhenReady = true;
    private long resumePosition = 0L;
    private final AtomicBoolean preparing = new AtomicBoolean(false);

    /* ---------------- constructor ---------------- */

    public VideoMediaController(@NonNull Context context, @NonNull PlayerView playerView) {
        this.context = context;
        this.view = playerView;
        this.view.setVisibility(View.GONE);
    }

    /* ---------------- public api ---------------- */

    public void setCallback(MediaLoadCallback callback) {
        this.callback = callback;
    }

    public void show(@NonNull AlbumMedia media) {
        if (this.media != null && !this.media.fileId.equals(media.fileId)) {
            resumePosition = 0L;
        }
        this.media = media;
        view.setVisibility(View.GONE);
    }

    /* ---------------- lifecycle ---------------- */

    public void onStart() {
        if (player == null && media != null) prepare();
    }

    public void onResume() {
        if (player != null) {
            player.setPlayWhenReady(playWhenReady);
        } else if (media != null) {
            prepare();
        }
    }

    public void onPause() {
        releasePlayerInternal();
    }

    public void onStop() {
        releasePlayerInternal();
    }

    public void release() {
        releasePlayerInternal();
        callback = null;
        media = null;
    }

    /* ---------------- prepare (lifecycle gate) ---------------- */

    private void prepare() {
        if (!preparing.compareAndSet(false, true)) return;

        view.setVisibility(View.GONE);
        if (callback != null) callback.onMediaLoading("Loading video…");

        /*
         * PREPARE SESSION STARTS HERE
         */
        session = new VideoMediaSession(context, this);
        session.start(media);

    }

    /* ---------------- attach (commit point) ---------------- */

    public void attach(@NonNull DefaultMediaSourceFactory factory,
                       @NonNull MediaItem item) {

        // ❌ DO NOT kill session here
        // releasePlayerInternal();

        // Only release existing player instance
        if (player != null) {
            resumePosition = player.getCurrentPosition();
            playWhenReady = player.getPlayWhenReady();
            player.release();
            player = null;
        }

        if (!preparing.compareAndSet(true, false)) return;

        player = buildPlayer(factory);
        view.setPlayer(player);
        player.setMediaItem(item);
        player.prepare();

        if (resumePosition > 0) player.seekTo(resumePosition);
    }



    /* ---------------- player ---------------- */

    private ExoPlayer buildPlayer(@NonNull DefaultMediaSourceFactory factory) {
        ExoPlayer p = new ExoPlayer.Builder(view.getContext())
                .setMediaSourceFactory(factory)
                .build();
        p.setRepeatMode(Player.REPEAT_MODE_ONE);
        p.setPlayWhenReady(playWhenReady);
        p.addListener(playerListener());
        return p;
    }

    private Player.Listener playerListener() {
        return new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY && player != null) {
                    session.onPlayerReady();
                    view.setVisibility(View.VISIBLE);
                    if (callback != null) callback.onMediaReady();
                }
            }
        };
    }

    /* ---------------- release ---------------- */

    private void releasePlayerInternal() {
        preparing.set(false);

        // 🔥 kill session first
        if (session != null) {
            session.release();
            session = null;
        }

        if (player == null) return;

        resumePosition = player.getCurrentPosition();
        playWhenReady = player.getPlayWhenReady();

        player.release();
        player = null;
    }
}
