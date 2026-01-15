package com.github.jaykkumar01.vaultspace.album.upload;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.github.jaykkumar01.vaultspace.core.session.cache.UploadCache;
import com.github.jaykkumar01.vaultspace.models.MediaSelection;

public final class AlbumUploadOrchestrator {

    private final Context appContext;
    private final UploadManager uploadManager;
    private final UploadCache uploadCache;

    private boolean serviceRunning;

    public AlbumUploadOrchestrator(
            @NonNull Context context,
            @NonNull UploadManager uploadManager,
            @NonNull UploadCache uploadCache
    ) {
        this.appContext = context.getApplicationContext();
        this.uploadManager = uploadManager;
        this.uploadCache = uploadCache;

        this.uploadManager.attachOrchestrator(this);
    }

    /* ================= UI-facing ================= */

    public void enqueue(
            @NonNull String albumId,
            @NonNull Iterable<MediaSelection> selections
    ) {
        uploadManager.enqueue(albumId, selections);
        // service lifecycle is driven ONLY by state changes
    }

    public void registerObserver(
            @NonNull String albumId,
            @NonNull UploadObserver observer
    ) {
        uploadManager.registerObserver(albumId, observer);
    }

    public void unregisterObserver(@NonNull String albumId) {
        uploadManager.unregisterObserver(albumId);
    }

    public void cancelAllUploads() {
        uploadManager.cancelAllByUser();
    }

    /* ================= UploadManager callback ================= */

    void onUploadStateChanged() {

        if (!serviceRunning) {
            startForegroundService();
            return;
        }

        // service already running → nudge only
        Intent intent = new Intent(appContext, UploadForegroundService.class);
        appContext.startService(intent);

        if (!uploadCache.hasAnyActiveUploads()) {
            stopForegroundServiceIfIdle();
        }
    }

    /* ================= Foreground service ================= */

    private void startForegroundService() {
        Intent intent = new Intent(
                appContext,
                UploadForegroundService.class
        );

        ContextCompat.startForegroundService(appContext, intent);
        serviceRunning = true;
    }

    private void stopForegroundServiceIfIdle() {
        if (!serviceRunning) return;
        if (uploadCache.hasAnyActiveUploads()) return;

        Intent intent = new Intent(
                appContext,
                UploadForegroundService.class
        );

        appContext.stopService(intent);
        serviceRunning = false;
    }
}
