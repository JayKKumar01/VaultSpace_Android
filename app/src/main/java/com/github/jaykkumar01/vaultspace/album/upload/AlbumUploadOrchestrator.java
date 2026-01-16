package com.github.jaykkumar01.vaultspace.album.upload;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.core.session.cache.UploadCache;
import com.github.jaykkumar01.vaultspace.models.MediaSelection;

import java.util.List;

public final class AlbumUploadOrchestrator {

    private static final String TAG = "VaultSpace:ForegroundAndOrchestrator";

    private static AlbumUploadOrchestrator INSTANCE;

    private final Context appContext;
    private final UploadManager uploadManager;
    private final UploadCache uploadCache;

    /* ================= Service State ================= */

    private enum ServiceState {
        IDLE,
        RUNNING,
        FINALIZING
    }

    private ServiceState serviceState = ServiceState.IDLE;

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

        Log.d(TAG, "Initialized. serviceState=IDLE");
    }

    /* ================= UI-facing ================= */

    public void enqueue(
            @NonNull String albumId,
            @NonNull String albumName,
            @NonNull List<MediaSelection> selections
    ) {
        Log.d(TAG, "enqueue(): albumId=" + albumId + ", items=" + selections.size());
        uploadManager.enqueue(albumId, albumName, selections);
    }

    public void registerObserver(
            @NonNull String albumId,
            @NonNull UploadObserver observer
    ) {
        Log.d(TAG, "registerObserver(): albumId=" + albumId);
        uploadManager.registerObserver(albumId, observer);
    }

    public void unregisterObserver(@NonNull String albumId) {
        Log.d(TAG, "unregisterObserver(): albumId=" + albumId);
        uploadManager.unregisterObserver(albumId);
    }

    public void cancelAllUploads() {
        Log.d(TAG, "cancelAllUploads()");

        uploadManager.cancelAllUploads();

        // 🔥 USER CANCEL → STOP SERVICE HARD
        stopForegroundServiceImmediately();
    }


    public void cancelUploads(String albumId) {
        Log.d(TAG, "cancelUploads(): albumId=" + albumId);
        // to be implemented later
    }

    public void retryUploads(String albumId) {
        Log.d(TAG, "retryUploads(): albumId=" + albumId);
        // to be implemented later
    }

    /* ================= UploadManager callback ================= */

    void onUploadStateChanged() {

        boolean hasActive = uploadCache.hasAnyActiveUploads();

        Log.d(TAG,
                "onUploadStateChanged(): state=" + serviceState +
                        ", hasActive=" + hasActive
        );

        switch (serviceState) {

            case IDLE:
                Log.d(TAG, "State=IDLE → starting foreground service");
                startForegroundService();
                serviceState = ServiceState.RUNNING;
                Log.d(TAG, "State changed → RUNNING");
                return;

            case RUNNING:
                // 🔑 ALWAYS nudge service
                Log.d(TAG, "State=RUNNING → nudging service");
                nudgeService();
                return;

            case FINALIZING:
                Log.d(TAG, "State=FINALIZING → ignoring upload state change");
                return;
        }
    }

    /* ================= Foreground service control ================= */

    private void startForegroundService() {
        Log.d(TAG, "startForegroundService()");
        Intent intent = new Intent(appContext, UploadForegroundService.class);
        ContextCompat.startForegroundService(appContext, intent);
    }

    private void nudgeService() {
        Log.d(TAG, "nudgeService()");
        Intent intent = new Intent(appContext, UploadForegroundService.class);
        appContext.startService(intent);
    }

    /* ================= Service callbacks ================= */

    void onServiceFinalizing() {
        Log.d(TAG, "onServiceFinalizing(): RUNNING → FINALIZING");
        serviceState = ServiceState.FINALIZING;
    }

    void onServiceDestroyed() {
        Log.d(TAG, "onServiceDestroyed(): FINALIZING → IDLE");
        serviceState = ServiceState.IDLE;
    }

    /* ================= Session cleanup ================= */

    public void onSessionCleared() {

        Log.d(TAG, "onSessionCleared(): cancelling uploads + stopping service");

        uploadManager.cancelAllUploads();

        Intent intent = new Intent(appContext, UploadForegroundService.class);
        appContext.stopService(intent);

        serviceState = ServiceState.IDLE;
        Log.d(TAG, "State reset → IDLE");
    }

    private void stopForegroundServiceImmediately() {
        Log.d(TAG, "Stopping foreground service immediately (user cancel)");

        Intent intent = new Intent(appContext, UploadForegroundService.class);
        appContext.stopService(intent);

        serviceState = ServiceState.IDLE;
        Log.d(TAG, "ServiceState reset → IDLE");
    }

}
