package com.github.jaykkumar01.vaultspace.core.download.service;

import static com.github.jaykkumar01.vaultspace.core.download.base.NotificationDetails.*;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.github.jaykkumar01.vaultspace.core.download.DownloadOrchestrator;
import com.github.jaykkumar01.vaultspace.core.download.base.DownloadRequest;

@RequiresApi(api = Build.VERSION_CODES.Q)
public final class DownloadService extends Service {

    /* ================= State ================= */

    private DownloadOrchestrator orchestrator;
    private boolean foregroundStarted;

    /* ================= Lifecycle ================= */

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        cleanupStalePendingDownloads();
        orchestrator = new DownloadOrchestrator(this);
        orchestrator.onServiceStarted();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        String action = intent.getAction();

        if (ACTION_START.equals(action)) {
            ensureForeground();

            DownloadRequest req =
                    intent.getParcelableExtra(EXTRA_DOWNLOAD_REQUEST);
            if (req != null) orchestrator.enqueue(req);

        } else if (ACTION_CANCEL.equals(action)) {
            orchestrator.cancelAll();
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        orchestrator.cancelAll();
        stopSelf();
    }

    @Override
    public void onDestroy() {
        orchestrator.onServiceDestroyed();
        orchestrator = null;
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /* ================= Foreground ================= */

    private void ensureForeground() {
        if (foregroundStarted) return;

        startForeground(
                NOTIFICATION_ID,
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.stat_sys_download)
                        .setContentTitle("Downloading…")
                        .setOnlyAlertOnce(true)
                        .build()
        );

        foregroundStarted = true;
    }

    /* ================= Pending Cleanup ================= */

    private void cleanupStalePendingDownloads() {
        ContentResolver resolver = getContentResolver();
        Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;

        try (Cursor c = resolver.query(
                collection,
                new String[]{MediaStore.MediaColumns._ID},
                MediaStore.MediaColumns.IS_PENDING + "=1",
                null,
                null
        )) {
            if (c == null) return;

            while (c.moveToNext()) {
                long id = c.getLong(0);
                Uri uri = Uri.withAppendedPath(collection, String.valueOf(id));
                resolver.delete(uri, null, null);
            }
        }
    }

    /* ================= Notification Channel ================= */

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Album Downloads",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("VaultSpace album media downloads");
        channel.setSound(null, null);
        channel.enableVibration(false);

        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.createNotificationChannel(channel);
    }
}
