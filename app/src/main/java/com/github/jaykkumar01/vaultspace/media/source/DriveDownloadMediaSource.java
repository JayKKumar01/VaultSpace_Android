package com.github.jaykkumar01.vaultspace.media.source;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.FileDataSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.github.jaykkumar01.vaultspace.media.base.VideoMediaPrepareCallback;
import com.github.jaykkumar01.vaultspace.media.base.VideoMediaSource;
import com.github.jaykkumar01.vaultspace.media.helper.DriveDownloadCache;
import com.google.api.services.drive.Drive;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@UnstableApi
public final class DriveDownloadMediaSource implements VideoMediaSource {

    /* ---------------- dependencies ---------------- */

    private final Drive drive;
    private final DriveDownloadCache cache;
    private final ExecutorService executor;
    private final Handler mainHandler;

    /* ---------------- state ---------------- */

    private final AtomicBoolean preparing = new AtomicBoolean(false);
    private volatile boolean released = false;

    /* ---------------- constructor ---------------- */

    public DriveDownloadMediaSource(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        this.drive = DriveClientProvider.getPrimaryDrive(appContext);
        this.cache = new DriveDownloadCache(appContext);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /* ---------------- VideoMediaSource ---------------- */

    @Override
    public void prepare(@NonNull AlbumMedia media,
                        @NonNull VideoMediaPrepareCallback callback) {

        if (released) return;
        if (!preparing.compareAndSet(false, true)) return;

        executor.execute(() -> {
            try {
                if (released) return;

                File file = cache.getFile(media.fileId);

                if (!file.exists()) {
                    long total = media.sizeBytes;

                    try (ProgressFileOutputStream os =
                                 new ProgressFileOutputStream(file, total, callback)) {

                        drive.files()
                                .get(media.fileId)
                                .executeMediaAndDownloadTo(os);
                    }
                }

                cache.touch(file);
                cache.evictIfNeeded();

                DefaultMediaSourceFactory factory =
                        new DefaultMediaSourceFactory(new FileDataSource.Factory());

                MediaItem item = MediaItem.fromUri(Uri.fromFile(file));

                mainHandler.post(() -> {
                    preparing.set(false);
                    if (!released)
                        callback.onReady(factory, item);
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    preparing.set(false);
                    if (!released)
                        callback.onError(e);
                });
            }
        });
    }

    @Override
    public void release() {
        released = true;
        executor.shutdownNow();
    }

    /* ---------------- internal progress stream ---------------- */

    private final class ProgressFileOutputStream extends FileOutputStream {

        private final long total;
        private long written;
        private final VideoMediaPrepareCallback callback;

        ProgressFileOutputStream(@NonNull File file,
                                 long total,
                                 @NonNull VideoMediaPrepareCallback cb) throws IOException {
            super(file);
            this.total = total;
            this.callback = cb;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            super.write(b, off, len);
            written += len;
            postProgress();
        }

        @Override
        public void write(int b) throws IOException {
            super.write(b);
            written++;
            postProgress();
        }

        private void postProgress() {
            if (released) return;
            mainHandler.post(() ->
                    callback.onProgress(written, total));
        }
    }
}
