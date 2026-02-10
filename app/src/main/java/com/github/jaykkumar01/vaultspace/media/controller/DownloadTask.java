package com.github.jaykkumar01.vaultspace.media.controller;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

final class DownloadTask implements VideoMediaTask {

    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    DownloadTask(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void start(@NonNull AlbumMedia media,
                      @NonNull Callback callback) {

        if (cancelled.get()) return;

        executor.execute(() -> {
            if (cancelled.get()) return;

            try {
                // 1️⃣ Download full file to disk (blocking)
                //    - Drive executeMediaAndDownloadTo
                //    - cache write
                //    - verify size

                DefaultMediaSourceFactory factory =
                        buildFileFactory(media);

                MediaItem item =
                        buildFileItem(media);

                // 2️⃣ SINGLE ATTACH (final)
                callback.onAttachReady(
                        new AttachPayload(factory,null, item)
                );

                callback.onHealthy();

            } catch (Exception e) {
                if (!cancelled.get()) {
                    callback.onError(e);
                }
            }
        });
    }

    @Override
    public void cancel() {
        cancelled.set(true);
        executor.shutdownNow();
    }

    /* ---------------- builders ---------------- */

    private DefaultMediaSourceFactory buildFileFactory(AlbumMedia media) {
        // FileDataSource.Factory + local cache
//        return new DefaultMediaSourceFactory(/* later */);
        return null;
    }

    private MediaItem buildFileItem(AlbumMedia media) {
        // Uri.fromFile(localFile)
//        return MediaItem.fromUri(/* local file uri */);
        return null;
    }
}
