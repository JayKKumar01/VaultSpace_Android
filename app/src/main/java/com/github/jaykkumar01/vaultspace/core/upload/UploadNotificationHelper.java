package com.github.jaykkumar01.vaultspace.core.upload;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.github.jaykkumar01.vaultspace.R;

import java.util.Map;

final class UploadNotificationHelper {

    private static final String TAG = "VaultSpace:ForegroundAndOrchestrator";
    private static final String CHANNEL_ID = "vaultspace_uploads";
    private static final long MIN_RENDER_INTERVAL_MS = 5_000;

    private final Context appContext;
    private final NotificationManager notificationManager;

    private UploadForegroundService.NotificationState lastRenderedState;
    private long lastRenderTimeMs;

    UploadNotificationHelper(Context context) {
        this.appContext = context.getApplicationContext();
        this.notificationManager =
                (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    /* ========================================================== */

    boolean shouldRender(long now, UploadForegroundService.NotificationState state) {
        return lastRenderedState != state
                || now - lastRenderTimeMs >= MIN_RENDER_INTERVAL_MS;
    }

    void renderForeground(
            int notificationId,
            Notification notification,
            UploadForegroundService.NotificationState state,
            long now
    ) {
        Log.d(TAG, "renderForeground(): state=" + state);
        notificationManager.notify(notificationId, notification);
        lastRenderedState = state;
        lastRenderTimeMs = now;
    }

    void postFinal(int notificationId, Notification notification) {
        Log.d(TAG, "postFinal(): id=" + notificationId);
        notificationManager.notify(notificationId, notification);
    }

    /* ========================================================== */

    Notification buildNotification(
            UploadForegroundService.NotificationState state,
            Map<String, UploadSnapshot> snapshots
    ) {

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(appContext, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_upload)
                        .setOnlyAlertOnce(true)
                        .setOngoing(state == UploadForegroundService.NotificationState.UPLOADING);

        switch (state) {

            case UPLOADING:
                builder.setContentTitle("Uploading media…")
                        .setContentText(buildUploadingText(snapshots));

                if (!snapshots.isEmpty()) {
                    builder.addAction(
                            R.drawable.ic_cancel,
                            "Cancel uploads",
                            buildCancelIntent()
                    );
                }
                break;

            case FINISHED_SUCCESS:
                builder.setContentTitle("Upload complete")
                        .setContentText("Media added successfully");
                break;

            case FINISHED_WITH_ISSUES:
                builder.setContentTitle("Upload finished with issues")
                        .setContentText(
                                countFailedAlbums(snapshots) + " albums need attention"
                        );
                break;

            case STOPPED_USER:
                builder.setContentTitle("Upload stopped")
                        .setContentText("Some items were not uploaded");
                break;

            case STOPPED_SYSTEM:
                builder.setContentTitle("Upload stopped")
                        .setContentText("Uploads couldn’t continue");
                break;
        }

        return builder.build();
    }

    /* ========================================================== */

    private PendingIntent buildCancelIntent() {
        Intent intent = new Intent(appContext, UploadForegroundService.class);
        intent.setAction(UploadForegroundService.ACTION_CANCEL);

        return PendingIntent.getService(
                appContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private String buildUploadingText(Map<String, UploadSnapshot> snapshots) {

        if (snapshots == null || snapshots.isEmpty()) {
            return "Preparing uploads…";
        }

        UploadSnapshot primary = null;
        int activeCount = 0;

        for (UploadSnapshot s : snapshots.values()) {
            if (s.uploaded + s.failed >= s.total) continue;
            if (primary == null) primary = s;
            activeCount++;
        }

        if (primary == null) {
            return "Finalizing uploads…";
        }

        StringBuilder sb = new StringBuilder(64);
        sb.append(primary.groupName)
                .append(" · ")
                .append(primary.uploaded)
                .append(" of ")
                .append(primary.total);

        if (activeCount > 1) {
            sb.append("\n+ ")
                    .append(activeCount - 1)
                    .append(" more album");
        }

        return sb.toString();
    }

    private int countFailedAlbums(Map<String, UploadSnapshot> snapshots) {
        int count = 0;
        for (UploadSnapshot s : snapshots.values()) {
            if (s.hasFailures()) count++;
        }
        return count;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Media uploads",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setShowBadge(false);
        notificationManager.createNotificationChannel(channel);
    }
}
