package com.github.jaykkumar01.vaultspace.core.upload;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.core.session.cache.UploadCache;
import com.github.jaykkumar01.vaultspace.models.base.UploadSelection;

import java.util.List;

/**
 * UploadOrchestrator
 *
 * UI-facing facade that:
 * - wires UploadManager
 * - controls foreground service lifecycle
 * - is agnostic of upload source (album, files, future)
 */
public final class UploadOrchestrator {

    private static final String TAG = "VaultSpace:UploadOrchestrator";

    private static UploadOrchestrator INSTANCE;

    private final Context appContext;
    private final UploadManagerOld uploadManagerOld;
    private final UploadCache uploadCache;

    /* ================= Service State ================= */

    private enum ServiceState {
        IDLE,
        RUNNING,
        FINALIZING
    }

    private ServiceState serviceState = ServiceState.IDLE;

    /* ================= Singleton ================= */

    public static synchronized UploadOrchestrator getInstance(
            @NonNull Context context
    ) {
        if (INSTANCE == null) {
            INSTANCE = new UploadOrchestrator(context);
        }
        return INSTANCE;
    }

    private UploadOrchestrator(@NonNull Context context) {
        this.appContext = context.getApplicationContext();

        UserSession session = new UserSession(appContext);

        this.uploadCache = session.getVaultCache().uploadCache;
        this.uploadManagerOld = new UploadManagerOld(
                appContext,
                uploadCache,
                session.getUploadRetryStore()
        );

        this.uploadManagerOld.attachOrchestrator(this);

        Log.d(TAG, "Initialized. serviceState=IDLE");
    }

    /* ================= UI-facing API ================= */

    public void enqueue(
            @NonNull String groupId,
            @NonNull String groupLabel,
            @NonNull List<? extends UploadSelection> selections
    ) {
        Log.d(TAG,
                "enqueue(): groupId=" + groupId +
                        ", items=" + selections.size()
        );

        uploadManagerOld.enqueue(groupId, groupLabel, selections);
    }

    public void registerObserver(
            @NonNull String groupId,
            @NonNull String groupLabel,
            @NonNull UploadObserver observer
    ) {
        Log.d(TAG, "registerObserver(): groupId=" + groupId);

        uploadManagerOld.registerObserver(groupId, groupLabel, observer);
    }

    public void unregisterObserver(@NonNull String groupId) {
        Log.d(TAG, "unregisterObserver(): groupId=" + groupId);
        uploadManagerOld.unregisterObserver(groupId);
    }

    public void cancelUploads(@NonNull String groupId) {
        Log.d(TAG, "cancelUploads(): groupId=" + groupId);
        uploadManagerOld.cancelUploads(groupId);
    }

    public void cancelAllUploads() {
        Log.d(TAG, "cancelAllUploads()");
        uploadManagerOld.cancelAllUploads();
    }

    public void retryUploads(@NonNull String groupId) {
        Log.d(TAG, "retryUploads(): groupId=" + groupId);
        // implemented later
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
                if (hasActive) {
                    Log.d(TAG, "State=IDLE → starting foreground service");
                    startForegroundService();
                    serviceState = ServiceState.RUNNING;
                    Log.d(TAG, "State changed → RUNNING");
                }
                return;

            case RUNNING:
                // Always nudge to keep notification in sync
                Log.d(TAG, "State=RUNNING → nudging service");
                nudgeForegroundService();
                return;

            case FINALIZING:
                Log.d(TAG, "State=FINALIZING → ignoring upload state change");
                return;
        }
    }

    /* ================= Foreground service control ================= */

    private void startForegroundService() {
        Intent intent = new Intent(appContext, UploadForegroundService.class);
        ContextCompat.startForegroundService(appContext, intent);
    }

    private void nudgeForegroundService() {
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

        uploadManagerOld.cancelAllUploads();

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
