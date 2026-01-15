package com.github.jaykkumar01.vaultspace.album.upload;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.github.jaykkumar01.vaultspace.R;

/**
 * UploadForegroundService
 *
 * Dumb lifecycle holder for uploads.
 *
 * Responsibilities:
 * - Keep process alive
 * - Show cancel action
 *
 * Non-responsibilities:
 * - Upload logic
 * - Cache access
 * - Retry logic
 * - State decisions
 */
public final class UploadForegroundService extends Service {

    private static final String CHANNEL_ID = "vaultspace_uploads";
    private static final int NOTIFICATION_ID = 1001;

    private static final String ACTION_CANCEL =
            "com.github.jaykkumar01.vaultspace.upload.CANCEL";

    private boolean isForeground;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && ACTION_CANCEL.equals(intent.getAction())) {
            handleCancel();
            return START_NOT_STICKY;
        }

        if (!isForeground) {
            startForeground(
                    NOTIFICATION_ID,
                    buildInitialNotification()
            );
            isForeground = true;
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isForeground = false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // not a bound service
    }

    /* ==========================================================
     * Cancel handling
     * ========================================================== */

    private void handleCancel() {
        AlbumUploadOrchestrator orchestrator =
                AlbumUploadOrchestrator.getInstance();

        if (orchestrator != null) {
            orchestrator.cancelAllUploads();
        }
    }

    /* ==========================================================
     * Notification
     * ========================================================== */

    private Notification buildInitialNotification() {

        PendingIntent cancelIntent = PendingIntent.getService(
                this,
                0,
                new Intent(this, UploadForegroundService.class)
                        .setAction(ACTION_CANCEL),
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_upload) // your icon
                .setContentTitle("Uploading media")
                .setContentText("Uploads in progress")
                .setOngoing(true)
                .addAction(
                        R.drawable.ic_cancel,
                        "Cancel uploads",
                        cancelIntent
                )
                .build();
    }

    /* ==========================================================
     * Notification channel
     * ========================================================== */

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Media uploads",
                NotificationManager.IMPORTANCE_LOW
        );

        channel.setDescription("Foreground service for media uploads");
        channel.setShowBadge(false);

        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.createNotificationChannel(channel);
        }
    }
}
