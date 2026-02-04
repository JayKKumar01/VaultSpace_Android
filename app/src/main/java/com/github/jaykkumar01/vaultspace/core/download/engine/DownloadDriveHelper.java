package com.github.jaykkumar01.vaultspace.core.download.engine;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.github.jaykkumar01.vaultspace.core.download.base.DownloadRequest;
import com.github.jaykkumar01.vaultspace.core.download.base.DriveDownloadCallback;
import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.google.api.services.drive.Drive;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DownloadDriveHelper {

    /* ================= Constants ================= */

    private static final int BUFFER_SIZE = 256 * 1024; // 256 KB

    /* ================= Dependencies ================= */

    private final Drive drive;
    private final ContentResolver resolver;

    /* ================= Constructor ================= */

    public DownloadDriveHelper(Context context) {
        Context app = context.getApplicationContext();
        this.drive = DriveClientProvider.getPrimaryDrive(app);
        this.resolver = app.getContentResolver();
    }

    /* ================= Streaming ================= */

    public void streamToUri(
            DownloadRequest req,
            Uri uri,
            DriveDownloadCallback callback,
            AtomicBoolean cancelled
    ) throws Exception {

        if (cancelled.get()) throw new InterruptedException("Cancelled before start");

        try (InputStream in =
                     drive.files().get(req.fileId).executeMediaAsInputStream();
             OutputStream out =
                     resolver.openOutputStream(uri, "w")) {

            if (out == null) throw new IllegalStateException("OutputStream is null");

            byte[] buffer = new byte[BUFFER_SIZE];
            long downloaded = 0;
            int read;

            while ((read = in.read(buffer)) != -1) {
                if (cancelled.get()) throw new InterruptedException("Cancelled");

                out.write(buffer, 0, read);
                downloaded += read;
                callback.onProgress(downloaded, req.sizeBytes);
            }

            out.flush();
        }
    }
}
