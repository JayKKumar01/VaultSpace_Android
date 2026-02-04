package com.github.jaykkumar01.vaultspace.core.download;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.github.jaykkumar01.vaultspace.core.download.base.DownloadRequest;
import com.github.jaykkumar01.vaultspace.core.download.base.DriveDownloadCallback;
import com.github.jaykkumar01.vaultspace.core.download.engine.DownloadDriveHelper;

import java.util.concurrent.atomic.AtomicBoolean;

@RequiresApi(api = Build.VERSION_CODES.Q)
public final class DownloadManager {

    private static final String TAG = "DL_FINAL";

    /* ================= Dependencies ================= */

    private final ContentResolver resolver;
    private final DownloadDriveHelper driveHelper;

    /* ================= Constructor ================= */

    public DownloadManager(Context context) {
        Context app = context.getApplicationContext();
        this.resolver = app.getContentResolver();
        this.driveHelper = new DownloadDriveHelper(app);
    }

    /* ================= Public API (LOCKED) ================= */

    public void downloadAndFinalize(
            DownloadRequest req,
            DriveDownloadCallback callback,
            AtomicBoolean cancelled
    ) {
        Uri uri = createMediaStoreUri(req.name);
        if (uri == null) {
            callback.onFailed(new IllegalStateException("MediaStore insert failed"));
            return;
        }

        Log.d(TAG, "download -> " + uri);

        try {
            driveHelper.streamToUri(req, uri, callback, cancelled);

            if (cancelled.get()) {
                resolver.delete(uri, null, null);
                return;
            }

            finalizeMediaStore(uri);
            callback.onCompleted();

        } catch (Exception e) {
            resolver.delete(uri, null, null);
            if (!cancelled.get()) callback.onFailed(e);
        }
    }

    /* ================= MediaStore ================= */

    private void finalizeMediaStore(Uri uri) {
        ContentValues v = new ContentValues();
        v.put(MediaStore.MediaColumns.IS_PENDING, 0);
        resolver.update(uri, v, null, null);
    }

    private Uri createMediaStoreUri(String displayName) {
        String name = uniqueName(displayName);
        Log.d(TAG, "resolved name -> " + name);

        ContentValues v = new ContentValues();
        v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        v.put(MediaStore.MediaColumns.MIME_TYPE, "*/*");
        v.put(MediaStore.MediaColumns.IS_PENDING, 1);

        // 🔥 REAL FIX: isolate from system Downloads
        v.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/VaultSpace");

        return resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
    }

    /* ================= Filename Handling ================= */

    private String uniqueName(String original) {
        String base, ext;
        int dot = original.lastIndexOf('.');
        if (dot > 0 && dot < original.length() - 1) {
            base = original.substring(0, dot);
            ext = original.substring(dot);
        } else {
            base = original;
            ext = "";
        }

        String name = base + ext;
        int i = 1;
        while (exists(name)) name = base + " (" + i++ + ")" + ext;
        return name;
    }

    private boolean exists(String name) {
        try (Cursor c = resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.MediaColumns._ID},
                MediaStore.MediaColumns.DISPLAY_NAME + "=?",
                new String[]{name},
                null
        )) {
            return c != null && c.moveToFirst();
        }
    }
}
