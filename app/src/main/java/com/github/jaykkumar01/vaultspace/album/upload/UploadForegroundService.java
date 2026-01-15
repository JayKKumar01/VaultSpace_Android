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
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.core.session.cache.UploadCache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class UploadForegroundService extends Service {

    private static final String CHANNEL_ID = "vaultspace_uploads";
    private static final int NOTIFICATION_ID = 1001;

    public static final String ACTION_CANCEL =
            "vaultspace.upload.CANCEL";

    private boolean isForeground;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && ACTION_CANCEL.equals(intent.getAction())) {
            // Forward cancel to orchestrator (authoritative)
            AlbumUploadOrchestrator orchestrator =
                    AlbumUploadOrchestratorHolder.get();

            if (orchestrator != null) {
                orchestrator.cancelAllUploads();
            }

            stopSelf();
            return START_NOT_STICKY;
        }

        Notification notification = buildNotification();

        if (!isForeground) {
            startForeground(NOTIFICATION_ID, notification);
            isForeground = true;
        } else {
            NotificationManager nm =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.notify(NOTIFICATION_ID, notification);
            }
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
        return null;
    }

    /* ==========================================================
     * Notification building
     * ========================================================== */

    private Notification buildNotification() {

        UserSession session = new UserSession(getApplicationContext());
        UploadCache cache = session.getVaultCache().uploadCache;

        Map<String, UploadSnapshot> snapshots = cache.getAllSnapshots();
        UploadCache.StopReason stopReason = cache.getStopReason();

        NotificationState state = deriveState(snapshots, stopReason);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_upload)
                        .setOnlyAlertOnce(true)
                        .setOngoing(state == NotificationState.UPLOADING);

        switch (state) {

            case UPLOADING:
                builder.setContentTitle("Uploading media");
                builder.setContentText(buildUploadingText(snapshots));
                builder.addAction(
                        R.drawable.ic_cancel,
                        "Cancel uploads",
                        buildCancelIntent()
                );
                break;

            case FINISHED_SUCCESS:
                builder.setContentTitle("Upload complete");
                builder.setContentText("Media added successfully");
                break;

            case FINISHED_WITH_ISSUES:
                builder.setContentTitle("Upload finished with issues");
                builder.setContentText(
                        countFailedAlbums(snapshots) + " albums need attention"
                );
                break;

            case STOPPED_USER:
                builder.setContentTitle("Upload stopped");
                builder.setContentText("Some items were not uploaded");
                break;

            case STOPPED_SYSTEM:
                builder.setContentTitle("Upload stopped");
                builder.setContentText("Uploads couldn’t continue");
                break;
        }

        return builder.build();
    }

    /* ==========================================================
     * State derivation
     * ========================================================== */

    private NotificationState deriveState(
            Map<String, UploadSnapshot> snapshots,
            UploadCache.StopReason stopReason
    ) {
        if (snapshots.isEmpty()) {
            return NotificationState.UPLOADING;
        }

        for (UploadSnapshot s : snapshots.values()) {
            if (s.isInProgress()) {
                return NotificationState.UPLOADING;
            }
        }

        if (stopReason == UploadCache.StopReason.USER) {
            return NotificationState.STOPPED_USER;
        }

        if (stopReason == UploadCache.StopReason.SYSTEM) {
            return NotificationState.STOPPED_SYSTEM;
        }

        for (UploadSnapshot s : snapshots.values()) {
            if (s.hasFailures()) {
                return NotificationState.FINISHED_WITH_ISSUES;
            }
        }

        return NotificationState.FINISHED_SUCCESS;
    }

    /* ==========================================================
     * Helpers
     * ========================================================== */

    private PendingIntent buildCancelIntent() {
        Intent intent = new Intent(this, UploadForegroundService.class);
        intent.setAction(ACTION_CANCEL);

        return PendingIntent.getService(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private String buildUploadingText(
            Map<String, UploadSnapshot> snapshots
    ) {
        if (snapshots.isEmpty()) {
            return "Preparing uploads…";
        }

        List<UploadSnapshot> list =
                new ArrayList<>(snapshots.values());

        StringBuilder sb = new StringBuilder();

        int shown = Math.min(2, list.size());
        for (int i = 0; i < shown; i++) {
            UploadSnapshot s = list.get(i);
            sb.append(s.albumId)
              .append(" · ")
              .append(s.uploaded)
              .append(" of ")
              .append(s.total);

            if (i < shown - 1) sb.append("\n");
        }

        if (list.size() > 2) {
            sb.append("\n+ ")
              .append(list.size() - 2)
              .append(" more album");
        }

        return sb.toString();
    }

    private int countFailedAlbums(
            Map<String, UploadSnapshot> snapshots
    ) {
        int count = 0;
        for (UploadSnapshot s : snapshots.values()) {
            if (s.hasFailures()) count++;
        }
        return count;
    }

    /* ==========================================================
     * Channel
     * ========================================================== */

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Media uploads",
                NotificationManager.IMPORTANCE_LOW
        );

        channel.setShowBadge(false);

        NotificationManager nm =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.createNotificationChannel(channel);
        }
    }

    /* ==========================================================
     * Internal enums
     * ========================================================== */

    private enum NotificationState {
        UPLOADING,
        FINISHED_SUCCESS,
        FINISHED_WITH_ISSUES,
        STOPPED_USER,
        STOPPED_SYSTEM
    }
}
