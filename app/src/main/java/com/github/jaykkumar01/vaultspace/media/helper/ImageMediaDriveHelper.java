package com.github.jaykkumar01.vaultspace.media.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.google.api.services.drive.Drive;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MediaDriveHelper
 *
 * Responsibilities:
 * - Download original image ONCE from Drive
 * - Decode bitmap off main thread
 * - Deliver bitmap on main thread
 */
public final class ImageMediaDriveHelper {

    public interface ImageBitmapCallback {
        void onReady(@NonNull Bitmap bitmap);
        void onError(@NonNull Exception e);
    }

    private static final String TAG = "VaultSpace:MediaDrive";

    private final Drive drive;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public ImageMediaDriveHelper(@NonNull Context context) {
        this.drive = DriveClientProvider.getPrimaryDrive(context);
    }

    /* ============================================================
     * Original image load (single hit)
     * ============================================================ */

    public void loadOriginalBitmap(
            @NonNull AlbumMedia media,
            @NonNull ImageBitmapCallback callback
    ) {
        executor.execute(() -> {
            if (cancelled.get()) return;

            try (InputStream in = drive.files()
                    .get(media.fileId)
                    .setAlt("media")
                    .setSupportsAllDrives(true)
                    .executeMediaAsInputStream()) {

                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inPreferredConfig = Bitmap.Config.ARGB_8888;

                Bitmap bmp = BitmapFactory.decodeStream(in, null, opts);
                if (bmp == null)
                    throw new RuntimeException("Bitmap decode failed");

                mainHandler.post(() -> callback.onReady(bmp));

            } catch (Exception e) {
                Log.e(TAG, "loadOriginalBitmap failed", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /* ============================================================
     * Lifecycle
     * ============================================================ */

    public void cancel() {
        cancelled.set(true);
        executor.shutdownNow();
    }
}
