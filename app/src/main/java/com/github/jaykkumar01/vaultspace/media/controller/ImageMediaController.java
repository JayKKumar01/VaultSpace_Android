package com.github.jaykkumar01.vaultspace.media.controller;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.media.base.MediaLoadCallback;
import com.github.jaykkumar01.vaultspace.media.helper.ImageMediaDriveHelper;
import com.github.jaykkumar01.vaultspace.views.creative.image.VaultImageView;

public final class ImageMediaController {
    private static final String TAG = "VaultSpace:ImageMedia";


    private final VaultImageView imageView;
    private final ImageMediaDriveHelper driveHelper;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private MediaLoadCallback callback;
    private Bitmap fullBitmap;

    public ImageMediaController(@NonNull Context context, @NonNull VaultImageView imageView) {
        this.imageView = imageView;
        this.driveHelper = new ImageMediaDriveHelper(context);
        imageView.setVisibility(View.GONE);
    }

    public void setCallback(MediaLoadCallback callback) {
        this.callback = callback;
    }

    public void show(@NonNull AlbumMedia media) {
        imageView.setVisibility(View.VISIBLE);
        if (callback != null) callback.onMediaLoading("Loading image…");


        Log.d(TAG, "media rotation = " + media.rotation + "°");

        driveHelper.loadOriginalBitmap(
                media,
                new ImageMediaDriveHelper.ImageBitmapCallback() {

                    @Override
                    public void onReady(@NonNull Bitmap bmp) {
                        Bitmap out = rotateIfNeeded(bmp, media.rotation);

                        mainHandler.post(() -> {
                            replaceWithOriginal(out);
                            if (callback != null) callback.onMediaReady();
                        });
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        if (callback != null) callback.onMediaError(e);
                    }
                }
        );
    }

    public void release() {
        if (fullBitmap != null && !fullBitmap.isRecycled()) {
            fullBitmap.recycle();
            fullBitmap = null;
        }
        driveHelper.cancel();
        callback = null;
    }

    /* ---------------- helpers ---------------- */

    private void replaceWithOriginal(@NonNull Bitmap bmp) {
        if (fullBitmap != null && !fullBitmap.isRecycled())
            fullBitmap.recycle();

        fullBitmap = bmp;
        imageView.setImageBitmapSafe(bmp);
    }

    private static Bitmap rotateIfNeeded(@NonNull Bitmap src, int rotation) {
        if (rotation == 0) return src;

        Matrix m = new Matrix();

        int r = Math.abs(rotation);

        if (rotation < 0) {
            // mirror first (EXIF-style)
            m.setScale(-1f, 1f);
        }

        m.postRotate(r);

        try {
            Bitmap out = Bitmap.createBitmap(
                    src, 0, 0, src.getWidth(), src.getHeight(), m, true
            );
            if (out != src) src.recycle();
            return out;
        } catch (Exception e) {
            return src;
        }
    }

}
