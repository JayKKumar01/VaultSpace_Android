package com.github.jaykkumar01.vaultspace.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;

import com.github.jaykkumar01.vaultspace.models.UriFileInfo;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class UriUtils1 {

    private UriUtils1() {
    }

    /**
     * Resolves best possible moment time for images & videos.
     * <p>
     * Contract:
     * - momentMillis = epoch millis OR -1
     * - prefers capture time over filesystem time
     * - never guesses
     */
    @NonNull
    public static UriFileInfo resolve(@NonNull Context context, @NonNull Uri uri) {

        ContentResolver cr = context.getContentResolver();

        String name = "unknown";
        long size = -1;
        long momentMillis = -1;

        String mime = cr.getType(uri);
        boolean isImage = mime != null && mime.startsWith("image/");
        boolean isVideo = mime != null && mime.startsWith("video/");

        /* ---------- Basic metadata ---------- */

        try (Cursor c = cr.query(
                uri,
                new String[]{
                        OpenableColumns.DISPLAY_NAME,
                        OpenableColumns.SIZE,
                        MediaStore.MediaColumns.DATE_MODIFIED
                },
                null, null, null
        )) {
            if (c != null && c.moveToFirst()) {
                int n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int s = c.getColumnIndex(OpenableColumns.SIZE);
                int m = c.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED);

                if (n != -1) name = c.getString(n);
                if (s != -1) size = c.getLong(s);

                if (m != -1) {
                    long sec = c.getLong(m);
                    if (sec > 0) momentMillis = sec * 1000L;
                }
            }
        } catch (Exception ignored) {
        }

        /* ---------- IMAGE: EXIF ---------- */

        if (momentMillis <= 0 && isImage) {
            long exif = extractExifTime(context, uri);
            if (exif > 0) momentMillis = exif;
        }

        /* ---------- VIDEO: DATE_TAKEN ---------- */

        if (momentMillis <= 0 && isVideo) {
            try (Cursor c = cr.query(
                    uri,
                    new String[]{MediaStore.Video.VideoColumns.DATE_TAKEN},
                    null, null, null
            )) {
                if (c != null && c.moveToFirst()) {
                    long taken = c.getLong(0);
                    if (taken > 0) momentMillis = taken;
                }
            } catch (Exception ignored) {
            }
        }

        /* ---------- VIDEO: METADATA ---------- */

        if (momentMillis <= 0 && isVideo) {
            long meta = extractVideoMetadataTime(context, uri);
            if (meta > 0) momentMillis = meta;
        }

        return new UriFileInfo(uri, name, size, momentMillis);
    }

    /* ================= Helpers ================= */

    private static long extractExifTime(Context ctx, Uri uri) {
        try (InputStream in = ctx.getContentResolver().openInputStream(uri)) {
            if (in == null) return -1;

            ExifInterface exif = new ExifInterface(in);

            String dt = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);
            if (dt == null) dt = exif.getAttribute(ExifInterface.TAG_DATETIME);
            if (dt == null) return -1;

            SimpleDateFormat f =
                    new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US);

            Date d = f.parse(dt);
            return d != null ? d.getTime() : -1;

        } catch (Exception ignored) {
            return -1;
        }
    }

    private static long extractVideoMetadataTime(Context ctx, Uri uri) {
        MediaMetadataRetriever r = new MediaMetadataRetriever();
        try {
            r.setDataSource(ctx, uri);

            String date = r.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DATE
            );
            if (date == null) return -1;

            date = date.replace("-", "").replace(":", "");

            SimpleDateFormat f =
                    new SimpleDateFormat(
                            "yyyyMMdd'T'HHmmss.SSS'Z'",
                            Locale.US
                    );
            f.setTimeZone(TimeZone.getTimeZone("UTC"));

            Date d = f.parse(date);
            return d != null ? d.getTime() : -1;

        } catch (Exception ignored) {
            return -1;
        } finally {
            try {
                r.release();
            } catch (Exception ignored) {
            }
        }
    }

    public static boolean isAccessible(@NonNull Context context, @NonNull Uri uri) {
        try (Cursor c = context.getContentResolver().query(
                uri,
                new String[]{OpenableColumns.DISPLAY_NAME},
                null, null, null
        )) {
            return c != null && c.moveToFirst();
        } catch (Exception e) {
            return false;
        }
    }
}
