package com.github.jaykkumar01.vaultspace.album.upload;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.core.session.cache.UploadCache;

import java.util.Map;

public final class UploadForegroundService extends Service {

    private static final String TAG = "VaultSpace:ForegroundAndOrchestrator";

    static final int FG_NOTIFICATION_ID = 1001;
    static final int FINAL_NOTIFICATION_ID = 1002;

    public static final String ACTION_CANCEL =
            "vaultspace.upload.CANCEL";

    private static final long FINAL_STOP_DELAY_MS = 1_500;

    /* ========================================================== */

    enum NotificationState {
        UPLOADING,
        FINISHED_SUCCESS,
        FINISHED_WITH_ISSUES,
        STOPPED_USER,
        STOPPED_SYSTEM
    }

    private boolean isForeground;
    private boolean finalizationPending;
    private boolean finalNotificationPosted;   // 🔥 FIX

    private UploadCache uploadCache;
    private UploadNotificationHelper notificationHelper;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingStopRunnable;

    /* ========================================================== */

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");

        uploadCache = new UserSession(getApplicationContext())
                .getVaultCache()
                .uploadCache;

        notificationHelper = new UploadNotificationHelper(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG,
                "onStartCommand(): isForeground=" + isForeground +
                        ", finalizationPending=" + finalizationPending +
                        ", finalNotificationPosted=" + finalNotificationPosted
        );

        /* ---------- ABSORB AFTER FINALIZATION ---------- */
        if (finalizationPending) {
            Log.d(TAG, "Already finalizing → ignoring");
            return START_NOT_STICKY;
        }

        /* ---------- Cancel ---------- */
        if (intent != null && ACTION_CANCEL.equals(intent.getAction())) {
            Log.d(TAG, "ACTION_CANCEL");
            AlbumUploadOrchestrator.getInstance(getApplicationContext())
                    .cancelAllUploads();
            return START_NOT_STICKY;
        }

        Map<String, UploadSnapshot> snapshots = uploadCache.getAllSnapshots();
        UploadCache.StopReason stopReason = uploadCache.getStopReason();

        NotificationState state = deriveState(snapshots, stopReason);
        Notification notification =
                notificationHelper.buildNotification(state, snapshots);

        long now = System.currentTimeMillis();

        /* ---------- Ensure foreground (1001) ---------- */
        if (!isForeground) {
            Log.d(TAG, "startForeground()");
            startForeground(FG_NOTIFICATION_ID, notification);
            isForeground = true;

            notificationHelper.renderForeground(
                    FG_NOTIFICATION_ID, notification, state, now
            );

            cancelPendingStop();
        }

        /* ---------- Uploading (1001 untouched) ---------- */
        if (state == NotificationState.UPLOADING) {
            cancelPendingStop();

            if (notificationHelper.shouldRender(now, state)) {
                notificationHelper.renderForeground(
                        FG_NOTIFICATION_ID, notification, state, now
                );
            }
            return START_STICKY;
        }

        /* ---------- FINALIZATION (ONE-WAY) ---------- */

        Log.d(TAG, "FINALIZATION: " + state);
        finalizationPending = true;

        AlbumUploadOrchestrator.getInstance(getApplicationContext())
                .onServiceFinalizing();

        stopForeground(false);
        isForeground = false;

        // 🔥 CRITICAL FIX: post FINAL (1002) ONLY ONCE
        if (!finalNotificationPosted) {
            finalNotificationPosted = true;
            notificationHelper.postFinal(
                    FINAL_NOTIFICATION_ID, notification
            );
        }

        scheduleDelayedStop();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        cancelPendingStop();

        AlbumUploadOrchestrator.getInstance(getApplicationContext())
                .onServiceDestroyed();

        isForeground = false;
        finalizationPending = false;
        finalNotificationPosted = false;

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /* ========================================================== */

    private void scheduleDelayedStop() {
        cancelPendingStop();
        pendingStopRunnable = () -> {
            Log.d(TAG, "stopSelf()");
            stopSelf();
        };
        handler.postDelayed(pendingStopRunnable, FINAL_STOP_DELAY_MS);
    }

    private void cancelPendingStop() {
        if (pendingStopRunnable != null) {
            handler.removeCallbacks(pendingStopRunnable);
            pendingStopRunnable = null;
        }
    }

    private NotificationState deriveState(
            Map<String, UploadSnapshot> snapshots,
            UploadCache.StopReason stopReason
    ) {

        if (stopReason == UploadCache.StopReason.USER || snapshots.isEmpty())
            return NotificationState.STOPPED_USER;

        if (stopReason == UploadCache.StopReason.SYSTEM)
            return NotificationState.STOPPED_SYSTEM;

        for (UploadSnapshot s : snapshots.values()) {
            if (s.isInProgress())
                return NotificationState.UPLOADING;
        }

        for (UploadSnapshot s : snapshots.values()) {
            if (s.hasFailures())
                return NotificationState.FINISHED_WITH_ISSUES;
        }

        return NotificationState.FINISHED_SUCCESS;
    }
}
