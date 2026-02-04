package com.github.jaykkumar01.vaultspace.core.download;

import static com.github.jaykkumar01.vaultspace.core.download.base.NotificationDetails.*;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.core.download.service.DownloadService;

public final class NotificationController {

    /* ================= Fields ================= */

    private final Context app;
    private final NotificationManager nm;
    private final Handler main = new Handler(Looper.getMainLooper());

    private NotificationCompat.Builder builder;
    private int lastPercent = -1;

    /* ================= Constructor ================= */

    public NotificationController(Context c) {
        this.app = c.getApplicationContext();
        this.nm = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /* ================= Public API ================= */

    public void showInitial(String name) {
        main.post(() -> {
            lastPercent = -1;
            builder = baseBuilder(name)
                    .setContentText("Starting download…")
                    .setProgress(0, 0, true)
                    .setOngoing(true);

            nm.notify(NOTIFICATION_ID, builder.build());
        });
    }

    public void updateProgress(String name, long downloaded, long total) {
        if (total <= 0) return;

        int percent = (int) ((downloaded * 100L) / total);
        if (percent == lastPercent) return;
        lastPercent = percent;

        main.post(() -> {
            if (builder == null) return;

            builder.setContentTitle(name)
                    .setContentText(percent + "% downloaded")
                    .setProgress(100, percent, false);

            nm.notify(NOTIFICATION_ID, builder.build());
        });
    }

    public void showCompleted(String name) {
        main.post(() -> {
            if (builder == null) return;

            builder.setContentTitle(name)
                    .setContentText("Download complete")
                    .setProgress(0, 0, false)
                    .setOngoing(false)
                    .clearActions();

            nm.notify(NOTIFICATION_ID, builder.build());
            clearState();
        });
    }

    public void showFailed(String name) {
        main.post(() -> {
            if (builder == null) return;

            builder.setContentTitle(name)
                    .setContentText("Download failed")
                    .setProgress(0, 0, false)
                    .setOngoing(false)
                    .clearActions();

            nm.notify(NOTIFICATION_ID, builder.build());
            clearState();
        });
    }

    public void dismiss() {
        main.post(() -> {
            nm.cancel(NOTIFICATION_ID);
            clearState();
        });
    }

    /* ================= Internal ================= */

    private NotificationCompat.Builder baseBuilder(String name) {
        return new NotificationCompat.Builder(app, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_upload)
                .setContentTitle(name)
                .setOnlyAlertOnce(true)
                .setAutoCancel(false)
                .addAction(
                        R.drawable.ic_cancel,
                        "Cancel",
                        cancelIntent()
                );
    }

    private PendingIntent cancelIntent() {
        Intent i = new Intent(app, DownloadService.class);
        i.setAction(ACTION_CANCEL);

        return PendingIntent.getService(
                app,
                1, // stable non-zero id
                i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private void clearState() {
        builder = null;
        lastPercent = -1;
    }
}
