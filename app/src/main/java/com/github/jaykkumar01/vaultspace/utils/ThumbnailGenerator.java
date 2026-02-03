package com.github.jaykkumar01.vaultspace.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.jaykkumar01.vaultspace.core.upload.base.UploadType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

public final class ThumbnailGenerator {

    /* ================= Constants ================= */

    private static final String TAG = "VaultSpace:ThumbGen";

    private static final float MAX_ASPECT = 16f / 9f;
    private static final float EXTREME_ASPECT = 3.0f;
    private static final int MIN_THUMB_DIM = 256;

    /* ================= Public API ================= */

    @Nullable
    public static String generate(
            @NonNull Context context,
            @NonNull Uri uri,
            @NonNull UploadType type,
            @NonNull File outputDir
    ) {
        long t0 = System.nanoTime();
        Bitmap bmp = null;

        try {
            bmp = (type == UploadType.VIDEO)
                    ? decodeVideo(context, uri)
                    : decodeImage(context, uri);

            if (bmp == null) return null;

            bmp = smartCropIfExtreme(bmp);
            bmp = clampToMinSide(bmp);

            File out = new File(outputDir, UUID.randomUUID() + ".jpg");
            try (FileOutputStream fos = new FileOutputStream(out)) {
                bmp.compress(Bitmap.CompressFormat.JPEG, 75, fos);
            }

            long ms = (System.nanoTime() - t0) / 1_000_000;
            Log.d(TAG, "Thumb gen (" + type + ") in " + ms + "ms");

            return out.getAbsolutePath();

        } catch (Throwable ignored) {
            return null;
        } finally {
            if (bmp != null) bmp.recycle();
        }
    }

    /* ================= Image ================= */

    @Nullable
    private static Bitmap decodeImage(@NonNull Context context, @NonNull Uri uri) throws Exception {

        // -------- API 28+ --------
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(context.getContentResolver(), uri),
                    (d, info, src) -> {
                        d.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);

                        int w = info.getSize().getWidth();
                        int h = info.getSize().getHeight();

                        if (!isExtremeAspect(w, h)) {
                            d.setTargetSampleSize(calculateSampleSize(w, h));
                        }
                    }
            );
        }

        // -------- Pre-P --------
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return null;

            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, o);

            o.inJustDecodeBounds = false;
            o.inSampleSize = isExtremeAspect(o.outWidth, o.outHeight)
                    ? 1
                    : calculateSampleSize(o.outWidth, o.outHeight);

            try (InputStream is2 = context.getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(is2, null, o);
            }
        }
    }

    /* ================= Video ================= */

    @Nullable
    private static Bitmap decodeVideo(@NonNull Context context, @NonNull Uri uri) {
        try (MediaMetadataRetriever r = new MediaMetadataRetriever()) {
            r.setDataSource(context, uri);
            return r.getFrameAtTime(-1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /* ================= Geometry ================= */

    @NonNull
    private static Bitmap smartCropIfExtreme(@NonNull Bitmap src) {
        int w = src.getWidth(), h = src.getHeight();
        float ratio = (float) w / h;

        if (ratio >= 1f / MAX_ASPECT && ratio <= MAX_ASPECT) return src;

        boolean wide = ratio > 1f;
        int newW = wide ? Math.round(h * MAX_ASPECT) : w;
        int newH = wide ? h : Math.round(w * MAX_ASPECT);
        int x = (w - newW) >> 1;
        int y = (h - newH) >> 1;

        Bitmap out = Bitmap.createBitmap(src, x, y, newW, newH);
        if (out != src) src.recycle();
        return out;
    }

    /* ================= Utils ================= */

    private static boolean isExtremeAspect(int w, int h) {
        float r = (float) w / h;
        return r > EXTREME_ASPECT || r < 1f / EXTREME_ASPECT;
    }

    private static int calculateSampleSize(int w, int h) {
        int s = 1;
        while ((w / s) > MIN_THUMB_DIM * 4 || (h / s) > MIN_THUMB_DIM * 4)
            s <<= 1;
        return s;
    }

    @NonNull
    private static Bitmap clampToMinSide(@NonNull Bitmap src) {
        int w = src.getWidth(), h = src.getHeight();
        int min = Math.min(w, h);
        if (min < MIN_THUMB_DIM) return src;

        boolean widthIsMin = w <= h;
        float ratio = (float) h / w;

        int outW = widthIsMin ? MIN_THUMB_DIM : Math.round(MIN_THUMB_DIM / ratio);
        int outH = widthIsMin ? Math.round(MIN_THUMB_DIM * ratio) : MIN_THUMB_DIM;

        Bitmap out = Bitmap.createScaledBitmap(src, outW, outH, true);
        if (out != src) src.recycle();
        return out;
    }

    private ThumbnailGenerator() {}
}
