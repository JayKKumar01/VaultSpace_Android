package com.github.jaykkumar01.vaultspace.media.controller;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.FileDataSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

final class CacheTask implements VideoMediaTask {

    private final Context context;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    CacheTask(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void start(@NonNull AlbumMedia media, @NonNull Callback callback) {

        if (cancelled.get()) return;

        File file = getCachedFile(media);

        if (file == null || !file.exists() || file.length() <= 0) {
            callback.onUnhealthy();
            return;
        }

        FileDataSource.Factory dataSourceFactory = new FileDataSource.Factory();

        DefaultMediaSourceFactory factory =
                new DefaultMediaSourceFactory(dataSourceFactory);

        MediaItem item = MediaItem.fromUri(file.toURI().toString());

        callback.onAttachReady(new AttachPayload(factory, null, item));
    }

    @Override
    public void cancel() {
        cancelled.set(true);
    }

    /* ---------------- internal ---------------- */

    private File getCachedFile(@NonNull AlbumMedia media) {
        // SINGLE SOURCE OF TRUTH
        // Replace later with your real cache helper
        return new File(context.getCacheDir(), media.fileId + ".mp4");
    }
}
