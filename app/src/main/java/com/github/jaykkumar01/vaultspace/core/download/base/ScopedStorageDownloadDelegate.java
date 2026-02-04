package com.github.jaykkumar01.vaultspace.core.download.base;

import static com.github.jaykkumar01.vaultspace.core.download.base.NotificationDetails.ACTION_START;
import static com.github.jaykkumar01.vaultspace.core.download.base.NotificationDetails.ACTION_CANCEL;
import static com.github.jaykkumar01.vaultspace.core.download.base.NotificationDetails.EXTRA_DOWNLOAD_REQUEST;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.core.download.service.DownloadService;

@RequiresApi(api = Build.VERSION_CODES.Q)
public final class ScopedStorageDownloadDelegate implements DownloadDelegate {

    /* ================= Fields ================= */

    private final Context app;

    /* ================= Constructor ================= */

    public ScopedStorageDownloadDelegate(Context context) {
        this.app = context.getApplicationContext();
    }

    /* ================= DownloadDelegate ================= */

    @Override
    public void enqueue(AlbumMedia media) {
        if (media == null) return;

        DownloadRequest req = new DownloadRequest(
                media.fileId,
                media.name,
                media.sizeBytes
        );

        Intent i = new Intent(app, DownloadService.class);
        i.setAction(ACTION_START);
        i.putExtra(EXTRA_DOWNLOAD_REQUEST, req);

        ContextCompat.startForegroundService(app, i);
    }

    @Override
    public void cancelAll() {
        Intent i = new Intent(app, DownloadService.class);
        i.setAction(ACTION_CANCEL);
        app.startService(i);
    }
}
