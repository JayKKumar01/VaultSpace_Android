package com.github.jaykkumar01.vaultspace.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
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

    // Stable, memory-safe size for batch processing
    private static final int THUMB_SIZE = 128;

    @Nullable
    public static String generate(
            @NonNull Context context,
            @NonNull Uri uri,
            @NonNull UploadType type,
            @NonNull File outputDir
    ) {
        Bitmap bmp = null;

        try {
            if (type == UploadType.VIDEO) {
                bmp = decodeVideo(context, uri);
            } else {
                bmp = decodeImage(context, uri);
            }

            if (bmp == null) return null;
            bmp = clampToThumbSize(bmp);


            File out = new File(
                    outputDir,
                    UUID.randomUUID().toString() + ".jpg"
            );

            try (FileOutputStream fos = new FileOutputStream(out)) {
                // JPEG is fastest + lowest overhead in loops
                bmp.compress(Bitmap.CompressFormat.JPEG, 75, fos);
            }

            return out.getAbsolutePath();

        } catch (Throwable ignored) {
            return null; // fail-soft, never crash batch
        } finally {
            if (bmp != null) bmp.recycle();
        }
    }

    /* ================= IMAGE ================= */

    @Nullable
    private static Bitmap decodeImage(
            @NonNull Context context,
            @NonNull Uri uri
    ) throws Exception {

        // Modern Android (9+): fastest + safest
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(
                            context.getContentResolver(),
                            uri
                    ),
                    (decoder, info, src) -> {
                        decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                        decoder.setTargetSampleSize(
                                calculateSampleSize(
                                        info.getSize().getWidth(),
                                        info.getSize().getHeight()
                                )
                        );
                    }
            );
        }

        // Legacy fallback (API 24–27)
        try (InputStream is =
                     context.getContentResolver().openInputStream(uri)) {

            if (is == null) return null;

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, opts);

            opts.inSampleSize =
                    calculateSampleSize(opts.outWidth, opts.outHeight);
            opts.inJustDecodeBounds = false;

            try (InputStream is2 =
                         context.getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(is2, null, opts);
            }
        }
    }

    /* ================= VIDEO ================= */

    @Nullable
    private static Bitmap decodeVideo(
            @NonNull Context context,
            @NonNull Uri uri
    ) {

        // Most reliable across all Android versions
        try (MediaMetadataRetriever mmr = new MediaMetadataRetriever()) {
            mmr.setDataSource(context, uri);
            return mmr.getFrameAtTime(
                    -1,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            );
        } catch (Throwable ignored) {
            return null;
        }
    }

    /* ================= UTILS ================= */

    // Sampling > resizing (speed + memory)
    private static int calculateSampleSize(int w, int h) {
        int size = 1;
        while ((w / size) > THUMB_SIZE * 2
                || (h / size) > THUMB_SIZE * 2) {
            size <<= 1;
        }
        return size;
    }

    @NonNull
    private static Bitmap clampToThumbSize(@NonNull Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();

        if (w <= THUMB_SIZE && h <= THUMB_SIZE)
            return src;

        float scale = Math.min(
                (float) THUMB_SIZE / w,
                (float) THUMB_SIZE / h
        );

        int nw = Math.round(w * scale);
        int nh = Math.round(h * scale);

        Bitmap out = Bitmap.createScaledBitmap(src, nw, nh, true);
        if (out != src) src.recycle();
        return out;
    }


    private ThumbnailGenerator() {}
}
