package com.github.jaykkumar01.vaultspace.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.jaykkumar01.vaultspace.core.upload.base.UploadType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

public final class ThumbnailGenerator {

    /* ================= Constants ================= */

    private static final int THUMB_SIZE = 256;

    /* ================= Public API ================= */

    @Nullable
    public static String generate(
            @NonNull Context context,
            @NonNull Uri uri,
            @NonNull UploadType type,
            @NonNull File outputDir
    ) {
        Bitmap bmp = null;
        try {
            bmp = (type == UploadType.VIDEO)
                    ? decodeVideo(context, uri)
                    : decodeImage(context, uri);

            if (bmp == null) return null;

            bmp = clampToThumbSize(bmp);

            File out = new File(outputDir, UUID.randomUUID() + ".jpg");
            try (FileOutputStream fos = new FileOutputStream(out)) {
                bmp.compress(Bitmap.CompressFormat.JPEG, 75, fos);
            }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(context.getContentResolver(), uri),
                    (d, info, src) -> {
                        d.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                        d.setTargetSampleSize(
                                calculateSampleSize(
                                        info.getSize().getWidth(),
                                        info.getSize().getHeight()
                                )
                        );
                    }
            );
        }

        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return null;

            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, o);

            o.inSampleSize = calculateSampleSize(o.outWidth, o.outHeight);
            o.inJustDecodeBounds = false;

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

    private static Bitmap applyRotation(@NonNull Bitmap src, int rotation) {
        if (rotation == 0) return src;
        Matrix m = new Matrix();
        m.postRotate(rotation);
        Bitmap out = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, true);
        if (out != src) src.recycle();
        return out;
    }

    /* ================= Utils ================= */

    private static int calculateSampleSize(int w, int h) {
        int s = 1;
        while ((w / s) > THUMB_SIZE * 2 || (h / s) > THUMB_SIZE * 2) s <<= 1;
        return s;
    }

    @NonNull
    private static Bitmap clampToThumbSize(@NonNull Bitmap src) {
        int w = src.getWidth(), h = src.getHeight();
        if (w <= THUMB_SIZE && h <= THUMB_SIZE) return src;

        float scale = Math.min((float) THUMB_SIZE / w, (float) THUMB_SIZE / h);
        Bitmap out = Bitmap.createScaledBitmap(
                src, Math.round(w * scale), Math.round(h * scale), true
        );
        if (out != src) src.recycle();
        return out;
    }

    private ThumbnailGenerator() {}
}
