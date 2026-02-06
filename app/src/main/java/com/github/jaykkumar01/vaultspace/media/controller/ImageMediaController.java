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
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.media.base.MediaLoadCallback;
import com.github.jaykkumar01.vaultspace.media.helper.ImageMediaDriveHelper;

public final class ImageMediaController {

    private static final String TAG = "VaultSpace:ImageCtrl";

    private final ImageView imageView;
    private final ImageMediaDriveHelper driveHelper;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private MediaLoadCallback callback;
    private Bitmap fullBitmap;
    private long tStart;

    public ImageMediaController(@NonNull Context context,
                                @NonNull ImageView imageView) {
        this.imageView = imageView;
        this.driveHelper = new ImageMediaDriveHelper(context);
        this.imageView.setVisibility(View.GONE);
    }

    public void setCallback(MediaLoadCallback callback) {
        this.callback = callback;
    }

    public void show(@NonNull AlbumMedia media) {
        imageView.setVisibility(View.VISIBLE);
        tStart = SystemClock.uptimeMillis();

        if (callback != null) callback.onMediaLoading();

        driveHelper.loadOriginalBitmap(media,
                new ImageMediaDriveHelper.ImageBitmapCallback() {

                    @Override
                    public void onReady(@NonNull Bitmap bmp) {
                        Bitmap out = rotateIfNeeded(bmp, media.rotation);
                        long tDrive = SystemClock.uptimeMillis();

                        mainHandler.post(() -> {
                            replaceWithOriginal(out);
                            if (callback != null) callback.onMediaReady();
                            Log.d(TAG,
                                    "final image +" +
                                            (SystemClock.uptimeMillis() - tStart) +
                                            "ms (drive +" + (tDrive - tStart) + "ms)"
                            );
                        });
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        Log.w(TAG, "image load failed", e);
                        if (callback != null) callback.onMediaError(e);
                    }
                });
    }

    public void release() {
        if (fullBitmap != null && !fullBitmap.isRecycled()) {
            fullBitmap.recycle();
            fullBitmap = null;
        }
        driveHelper.cancel();
        callback = null;
    }

    private void replaceWithOriginal(@NonNull Bitmap bmp) {
        if (fullBitmap != null && !fullBitmap.isRecycled())
            fullBitmap.recycle();

        fullBitmap = bmp;

        Glide.with(imageView)
                .load(fullBitmap)
                .transition(DrawableTransitionOptions.withCrossFade(180))
                .into(imageView);
    }

    private static Bitmap rotateIfNeeded(@NonNull Bitmap src, int r) {
        if (r == 0) return src;
        Matrix m = new Matrix();
        m.setRotate(r);
        return Bitmap.createBitmap(
                src, 0, 0, src.getWidth(), src.getHeight(), m, true
        );
    }
}
