package com.github.jaykkumar01.vaultspace.album.upload;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.core.session.cache.UploadCache;
import com.github.jaykkumar01.vaultspace.models.MediaSelection;

import java.util.List;

public final class AlbumUploadOrchestrator {

    private static AlbumUploadOrchestrator INSTANCE;

    private final Context appContext;
    private final UploadManager uploadManager;
    private final UploadCache uploadCache;

    private boolean serviceRunning;

    /* ================= Singleton ================= */

    public static synchronized AlbumUploadOrchestrator getInstance(
            @NonNull Context context
    ) {
        if (INSTANCE == null) {
            INSTANCE = new AlbumUploadOrchestrator(context);
        }
        return INSTANCE;
    }

    private AlbumUploadOrchestrator(@NonNull Context context) {
        this.appContext = context.getApplicationContext();

        UserSession session = new UserSession(appContext);

        this.uploadCache = session.getVaultCache().uploadCache;
        this.uploadManager = new UploadManager(
                uploadCache,
                session.getUploadRetryStore()
        );

        this.uploadManager.attachOrchestrator(this);
    }

    /* ================= UI-facing ================= */

    public void enqueue(
            @NonNull String albumId,
            @NonNull String albumName,
            @NonNull List<MediaSelection> selections
    ) {
        uploadManager.enqueue(albumId, albumName, selections);
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
        uploadManager.cancelAllUploads();
    }

    /* ================= UploadManager callback ================= */

    void onUploadStateChanged() {

        if (!serviceRunning) {
            startForegroundService();
            return;
        }

        // service already running → nudge only
        Intent intent = new Intent(appContext, UploadForegroundService.class);

        if (uploadCache.hasAnyActiveUploads()) {
            appContext.startService(intent);
            return;
        }
        stopForegroundServiceIfIdle(intent);
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

    private void stopForegroundServiceIfIdle(Intent intent) {
        if (!serviceRunning) return;

        appContext.stopService(intent);
        serviceRunning = false;
    }

    public void onSessionCleared() {

        // 1️⃣ Cancel uploads if anything is running
        uploadManager.cancelAllUploads();

        // 2️⃣ Force-stop foreground service (defensive)
        Intent intent = new Intent(appContext, UploadForegroundService.class);
        appContext.stopService(intent);
    }

    public void cancelUploads(String albumId) {
    }

    public void retryUploads(String albumId) {

    }

    void onServiceDestroyed() {
        serviceRunning = false;
    }

}
