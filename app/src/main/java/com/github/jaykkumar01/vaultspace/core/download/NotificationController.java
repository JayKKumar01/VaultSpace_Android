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

    /* ================= Core ================= */

    private static final long MIN_UPDATE_MS = 500;
    private final Context app;
    private final NotificationManager nm;
    private final Handler main = new Handler(Looper.getMainLooper());

    private int lastPercent = -1;
    private long lastUpdateTime = 0;

    /* ================= Constructor ================= */

    public NotificationController(Context context) {
        this.app = context.getApplicationContext();
        this.nm = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /* ================= Foreground ================= */

    public void startForeground(String name, int pendingCount) {
        main.post(() -> {
            resetInternal();

            NotificationCompat.Builder b =
                    foregroundBuilder(name)
                            .setContentText(queueText(pendingCount))
                            .setProgress(0, 0, true);

            nm.notify(FG_NOTIFICATION_ID, b.build());
        });
    }

    public void updateProgress(String name, long downloaded, long total, int pendingCount) {
        if (total <= 0) return;

        long now = System.currentTimeMillis();
        if (now - lastUpdateTime < MIN_UPDATE_MS) return;
        lastUpdateTime = now;

        int percent = (int) ((downloaded * 100L) / total);
        if (percent == lastPercent) return;
        lastPercent = percent;

        main.post(() -> {
            NotificationCompat.Builder b =
                    foregroundBuilder(name)
                            .setContentText(queueText(pendingCount))
                            .setProgress(100, percent, false);

            nm.notify(FG_NOTIFICATION_ID, b.build());
        });
    }

    public void stopForeground() {
        main.post(() -> nm.cancel(FG_NOTIFICATION_ID));
    }

    /* ================= Results ================= */

    public void showCompletionSummary(
            int success,
            int failure,
            String singleFileName
    ) {
        if (success == 0 && failure == 0) return;

        String text;
        if (success == 1 && failure == 0 && singleFileName != null) {
            text = "Downloaded: " + singleFileName;
        } else if (success > 0 && failure == 0) {
            text = success + " files downloaded";
        } else if (success > 0) {
            text = success + " downloaded, " + failure + " failed";
        } else {
            text = failure == 1 ? "1 file failed" : failure + " files failed";
        }

        NotificationCompat.Builder b =
                new NotificationCompat.Builder(app, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_upload)
                        .setContentTitle("VaultSpace")
                        .setContentText(text)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        nm.notify(RESULT_NOTIFICATION_ID, b.build());
    }

    /* ================= Internal ================= */

    public void reset() {
        resetInternal();
    }

    private void resetInternal() {
        lastPercent = -1;
        lastUpdateTime = 0;
    }

    private String queueText(int pendingCount) {
        return pendingCount <= 0
                ? "Finishing…"
                : "Downloading… " + pendingCount + " remaining";
    }

    private NotificationCompat.Builder foregroundBuilder(String name) {
        return new NotificationCompat.Builder(app, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_upload)
                .setContentTitle(name)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setAutoCancel(false)
                .addAction(R.drawable.ic_cancel, "Cancel", cancelIntent());
    }

    private PendingIntent cancelIntent() {
        Intent i = new Intent(app, DownloadService.class);
        i.setAction(ACTION_CANCEL);
        return PendingIntent.getService(
                app, 1, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
