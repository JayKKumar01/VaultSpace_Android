package com.github.jaykkumar01.vaultspace.media.controller;

import static android.view.View.GONE;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
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
import com.github.jaykkumar01.vaultspace.media.datasource.DriveHttpDataSource;
import com.github.jaykkumar01.vaultspace.media.prefetch.DrivePrefetcher;

@UnstableApi
public final class VideoMediaController {

    private static final String TAG = "VideoMedia";
    private static final String SCHEME = "vaultspace://drive/";

    private final Context context;
    private final PlayerView view;
    private final MediaLoadCallback callback;
    private final DriveAltMediaCache cache;
    private final Handler main = new Handler(Looper.getMainLooper());

    private ExoPlayer player;
    private AlbumMedia media;
    private DrivePrefetcher prefetcher;
    private CacheDataSource.Factory cacheFactory;

    private boolean playWhenReady = true;
    private long resumePosition = 0L;
    private long startNs;

    public VideoMediaController(@NonNull Context ctx, @NonNull PlayerView view, @NonNull MediaLoadCallback cb) {
        this.context = ctx.getApplicationContext();
        this.view = view;
        this.callback = cb;
        this.cache = new DriveAltMediaCache(context);
        view.setVisibility(GONE);
    }

    public void show(@NonNull AlbumMedia media) {
        this.media = media;
        DataSource.Factory upstream = () -> new DriveHttpDataSource(context, media.fileId);
        this.cacheFactory = cache.wrap(media.fileId, upstream);
        this.prefetcher = new DrivePrefetcher(cacheFactory);
        view.setVisibility(GONE);
    }

    public void onStart() {
        if (player != null || media == null) return;
        startNs = System.nanoTime();
        Log.d(TAG, "Start loading fileId=" + media.fileId);
        callback.onMediaLoading("Loading video…");
        prefetcher.prefetch(context, media, () -> main.post(this::preparePlayer));
    }

    public void onResume() { if (player != null) player.setPlayWhenReady(playWhenReady); }
    public void onPause() { releasePlayer(); }

    public void onStop() {
        if (prefetcher != null) prefetcher.cancel();
        releasePlayer();
    }

    public void release() {
        if (prefetcher != null) prefetcher.cancel();
        releasePlayer();
        media = null;
    }

    private void preparePlayer() {
        ProgressiveMediaSource mediaSource =
                new ProgressiveMediaSource.Factory(cacheFactory)
                        .createMediaSource(MediaItem.fromUri(SCHEME + media.fileId));

        DefaultLoadControl loadControl =
                new DefaultLoadControl.Builder()
                        .setBufferDurationsMs(5000, 30000, 1000, 2000)
                        .build();

        player = new ExoPlayer.Builder(view.getContext())
                .setLoadControl(loadControl)
                .build();

        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        player.setPlayWhenReady(playWhenReady);
        player.setMediaSource(mediaSource);
        player.prepare();

        player.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    long readyMs = (System.nanoTime() - startNs) / 1_000_000L;
                    Log.d(TAG, "READY fileId=" + media.fileId + " timeMs=" + readyMs);
                    main.post(() -> {
                        if (view.getPlayer() == null) view.setPlayer(player);
                        view.setVisibility(View.VISIBLE);
                        callback.onMediaReady();
                    });
                }
            }
        });

        if (resumePosition > 0) player.seekTo(resumePosition);
    }

    private void releasePlayer() {
        if (player == null) return;
        resumePosition = player.getCurrentPosition();
        playWhenReady = player.getPlayWhenReady();
        view.setPlayer(null);
        player.release();
        player = null;
    }
}
