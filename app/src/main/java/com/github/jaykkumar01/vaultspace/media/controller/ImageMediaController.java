package com.github.jaykkumar01.vaultspace.media.controller;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.github.jaykkumar01.vaultspace.album.helper.DriveResolver;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.media.helper.ImageMediaDriveHelper;

/**
 * ImageMediaController
 *
 * UX:
 * - Instant thumbnail
 * - Smooth crossfade to final image
 *
 * Logs:
 * - Thumbnail time
 * - Drive load time
 * - UI replace time
 */
public final class ImageMediaController {

    private static final String TAG = "VaultSpace:ImageCtrl";

    private final ImageView imageView;
    private final ImageMediaDriveHelper driveHelper;
    private final DriveResolver resolver;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private Bitmap fullBitmap;

    // timing
    private long tStart;

    public ImageMediaController(Context context,ImageView imageView) {
        this.imageView = imageView;
        this.driveHelper = new ImageMediaDriveHelper(context);
        this.resolver = new DriveResolver(context);
    }

    /* ============================================================
     * Public API
     * ============================================================ */

    public void show(@NonNull AlbumMedia media) {
        imageView.setVisibility(View.VISIBLE);
        tStart = SystemClock.uptimeMillis();

        Log.d(TAG, "show() start");

        // 1️⃣ Thumbnail (instant)
        resolver.resolveAsync(media, path -> {
            if (path == null) return;

            mainHandler.post(() -> {
                Glide.with(imageView)
                        .load(path)
                        .dontAnimate()
                        .into(imageView);

                Log.d(TAG,
                        "thumbnail shown +" +
                                (SystemClock.uptimeMillis() - tStart) + "ms"
                );
            });
        });

        // 2️⃣ Original image (single Drive hit)
        driveHelper.loadOriginalBitmap(media, new ImageMediaDriveHelper.ImageBitmapCallback() {
            @Override
            public void onReady(@NonNull Bitmap bmp) {
                long tDrive = SystemClock.uptimeMillis();
                Log.d(TAG,
                        "drive bitmap ready +" + (tDrive - tStart) + "ms"
                );

                Bitmap out = rotateIfNeeded(bmp, media.rotation);

                mainHandler.post(() -> replaceWithOriginal(out, tDrive));
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.w(TAG, "original image load failed", e);
            }
        });
    }

    public void release() {
        if (fullBitmap != null && !fullBitmap.isRecycled()) {
            fullBitmap.recycle();
            fullBitmap = null;
        }
        driveHelper.cancel();
    }

    /* ============================================================
     * Internal helpers
     * ============================================================ */

    private void replaceWithOriginal(@NonNull Bitmap bmp,long tDrive) {
        if (fullBitmap != null && !fullBitmap.isRecycled())
            fullBitmap.recycle();

        fullBitmap = bmp;

        Glide.with(imageView)
                .load(fullBitmap)
                .transition(DrawableTransitionOptions.withCrossFade(180))
                .into(imageView);

        long now = SystemClock.uptimeMillis();
        Log.d(TAG,
                "final image shown +" +
                        (now - tStart) + "ms (ui +" + (now - tDrive) + "ms)"
        );
    }

    /**
     * Efficient rotation:
     * - Zero cost when rotation == 0
     * - Single allocation otherwise
     */
    private static Bitmap rotateIfNeeded(@NonNull Bitmap src,int rotation) {
        if (rotation == 0) return src;

        Matrix m = new Matrix();
        m.setRotate(rotation);

        return Bitmap.createBitmap(
                src, 0, 0,
                src.getWidth(),
                src.getHeight(),
                m,
                true
        );
    }
}
