package com.github.jaykkumar01.vaultspace.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.jaykkumar01.vaultspace.models.base.UploadType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

public final class UploadThumbnailGenerator {

    private static final int THUMB_SIZE = 256;

    @Nullable
    public static String generate(
            @NonNull Context context,
            @NonNull Uri uri,
            @NonNull UploadType type,
            @NonNull File outputDir
    ) {
        Bitmap bmp = null;

        try {
            if (type == UploadType.PHOTO) {
                bmp = decodeImage(context, uri);
            } else if (type == UploadType.VIDEO) {
                bmp = decodeVideo(context, uri);
            }

            if (bmp == null) return null;

            File out = new File(
                    outputDir,
                    UUID.randomUUID().toString() + ".webp"
            );

            try (FileOutputStream fos = new FileOutputStream(out)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bmp.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, fos);
                } else {
                    bmp.compress(Bitmap.CompressFormat.WEBP, 90, fos);
                }
            }

            return out.getAbsolutePath();

        } catch (Throwable t) {
            return null; // fail-soft by design
        } finally {
            if (bmp != null) bmp.recycle();
        }
    }

    /* ================= Image ================= */

    @Nullable
    private static Bitmap decodeImage(
            @NonNull Context context,
            @NonNull Uri uri
    ) throws Exception {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(context.getContentResolver(), uri),
                    (decoder, info, src) -> {
                        decoder.setTargetSize(THUMB_SIZE, THUMB_SIZE);
                        decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                    }
            );
        }

        // API 24–27 fallback
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return null;

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, opts);

            opts.inSampleSize = calculateInSampleSize(
                    opts.outWidth,
                    opts.outHeight
            );
            opts.inJustDecodeBounds = false;

            try (InputStream is2 =
                         context.getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(is2, null, opts);
            }
        }
    }

    /* ================= Video ================= */

    @Nullable
    private static Bitmap decodeVideo(
            @NonNull Context context,
            @NonNull Uri uri
    ) {
        // API 29+ fast path
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                return context.getContentResolver().loadThumbnail(
                        uri,
                        new Size(THUMB_SIZE, THUMB_SIZE),
                        null
                );
            } catch (Throwable ignored) {}
        }

        // Universal fallback
        try (MediaMetadataRetriever mmr = new MediaMetadataRetriever()) {
            mmr.setDataSource(context, uri);
            Bitmap frame = mmr.getFrameAtTime(
                    0,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            );
            if (frame == null) return null;

            return Bitmap.createScaledBitmap(
                    frame,
                    THUMB_SIZE,
                    THUMB_SIZE,
                    true
            );
        } catch (Throwable t) {
            return null;
        }
    }

    /* ================= Utils ================= */

    private static int calculateInSampleSize(
            int width,
            int height
    ) {
        int inSampleSize = 1;
        while ((height / inSampleSize) > UploadThumbnailGenerator.THUMB_SIZE
                || (width / inSampleSize) > UploadThumbnailGenerator.THUMB_SIZE) {
            inSampleSize *= 2;
        }
        return inSampleSize;
    }

    private UploadThumbnailGenerator() {}
}
