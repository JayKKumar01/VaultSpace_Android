package com.github.jaykkumar01.vaultspace.album.helper;

import android.content.Context;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.core.download.base.ScopedStorageDownloadDelegate;
import com.github.jaykkumar01.vaultspace.core.download.engine.LegacyDownloadManager;
import com.github.jaykkumar01.vaultspace.core.download.base.DownloadDelegate;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;

public final class AlbumMediaActionHandler {

    /* ================= Fields ================= */

    private final DownloadDelegate delegate;
    private boolean released;

    /* ================= Constructor ================= */

    public AlbumMediaActionHandler(AppCompatActivity activity) {
        Context appContext = activity.getApplicationContext();
        this.delegate = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? new ScopedStorageDownloadDelegate(appContext)
                : new LegacyDownloadManager(appContext);
    }

    /* ================= Download Actions ================= */

    public void downloadMedia(AlbumMedia media) {
        if (released || media == null) return;
        delegate.enqueue(media);
    }

    public void cancelAllDownloads() {
        if (released) return;
        delegate.cancelAll();
    }

    /* ================= Lifecycle ================= */

    public void release() {
        if (released) return;
        released = true;
        delegate.cancelAll();
    }

    /* ================= Non-goal Placeholder ================= */

    public void deleteMedia(String id, Runnable onSuccess, Runnable onFailure) {
        try {
            if (onSuccess != null) onSuccess.run();
        } catch (Exception e) {
            if (onFailure != null) onFailure.run();
        }
    }
}
