package com.github.jaykkumar01.vaultspace.album.upload;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.github.jaykkumar01.vaultspace.core.session.cache.UploadCache;
import com.github.jaykkumar01.vaultspace.models.MediaSelection;

/**
 * AlbumUploadOrchestrator
 *
 * Android-aware bridge between UI and UploadManager.
 *
 * Responsibilities:
 * - Start / stop ForegroundService
 * - Forward UI intents to UploadManager
 * - Keep UploadManager Android-free
 *
 * Non-responsibilities:
 * - Upload execution
 * - Retry logic
 * - Snapshot inspection
 * - Notification state decisions
 */
public final class AlbumUploadOrchestrator {
    private static AlbumUploadOrchestrator INSTANCE;


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

        INSTANCE = this;
    }

    static AlbumUploadOrchestrator getInstance() {
        return INSTANCE;
    }


    /* ==========================================================
     * UI-facing APIs
     * ========================================================== */

    public void enqueue(
            @NonNull String albumId,
            @NonNull Iterable<MediaSelection> selections
    ) {
        uploadManager.enqueue(albumId, selections);
        ensureForegroundServiceRunning();
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
        stopForegroundServiceIfIdle();
    }

    /* ==========================================================
     * UploadManager → Orchestrator hooks
     * ========================================================== */

    /**
     * Called by UploadManager when uploads reach a terminal state
     * (queue empty + no current task).
     */
    void onUploadsTerminated() {
        stopForegroundServiceIfIdle();
    }

    /* ==========================================================
     * Foreground service control
     * ========================================================== */

    private void ensureForegroundServiceRunning() {
        if (serviceRunning) return;

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
