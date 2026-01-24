package com.github.jaykkumar01.vaultspace.utils;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;

import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadType;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

public final class UriUtils {

    private UriUtils() {}

    /* ============================================================
     * Public API
     * ============================================================ */

    public static boolean isPermissionRevoked(@NonNull Context context, @NonNull Uri uri) {
        try (Cursor c = context.getContentResolver().query(
                uri,
                new String[]{OpenableColumns.DISPLAY_NAME},
                null, null, null
        )) {
            return c == null || !c.moveToFirst();
        } catch (Exception e) {
            return true;
        }
    }

    @NonNull
    public static UploadSelection resolve(@NonNull Context context, @NonNull String groupId, @NonNull Uri uri) {
        ContentResolver cr = context.getContentResolver();

        BaseMeta meta = resolveBaseMeta(cr, uri);
        String mime = cr.getType(uri);
        UploadType type = mime != null ? UploadType.fromMime(mime) : UploadType.FILE;

        long moment =
                resolveMomentMillis(context, cr, uri, type, meta.momentMillis);

        String thumbPath = null;
        if (type == UploadType.PHOTO || type == UploadType.VIDEO) {
            File dir = new File(context.getCacheDir(), "thumbs");
            @SuppressLint("ResultOfMethodCallIgnored")
            boolean ignored = dir.exists() || dir.mkdirs();
            thumbPath = ThumbnailGenerator.generate(context, uri, type, dir);
        }

        String id = UUID.randomUUID().toString();
        return new UploadSelection(
                id,groupId,uri, mime, meta.displayName, meta.sizeBytes, moment, thumbPath
        );
    }

    /* ============================================================
     * Base metadata (single query)
     * ============================================================ */

    private static BaseMeta resolveBaseMeta(ContentResolver cr, Uri uri) {
        String name = "unknown";
        long size = -1;
        long moment = -1;

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
                    if (sec > 0) moment = sec * 1000L;
                }
            }
        } catch (Exception ignored) {}

        return new BaseMeta(name, size, moment);
    }

    /* ============================================================
     * Moment resolution (optimized, switch-based)
     * ============================================================ */

    private static long resolveMomentMillis(
            Context ctx,
            ContentResolver cr,
            Uri uri,
            UploadType type,
            long baseMoment
    ) {
        if (baseMoment > 0) return baseMoment;

        return switch (type) {
            case PHOTO -> {
                long exif = extractExifTime(ctx, uri);
                yield exif > 0 ? exif : -1;
            }
            case VIDEO -> {
                long taken = extractVideoDateTaken(cr, uri);
                if (taken > 0) yield taken;

                long meta = extractVideoMetadataTime(ctx, uri);
                yield meta > 0 ? meta : -1;
            }
            default -> -1;
        };
    }

    /* ============================================================
     * Image EXIF
     * ============================================================ */

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

    /* ============================================================
     * Video metadata
     * ============================================================ */

    private static long extractVideoDateTaken(ContentResolver cr, Uri uri) {
        try (Cursor c = cr.query(
                uri,
                new String[]{MediaStore.Video.VideoColumns.DATE_TAKEN},
                null, null, null
        )) {
            if (c != null && c.moveToFirst()) {
                long taken = c.getLong(0);
                if (taken > 0) return taken;
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private static long extractVideoMetadataTime(Context ctx, Uri uri) {
        try (MediaMetadataRetriever r = new MediaMetadataRetriever()) {

            r.setDataSource(ctx, uri);

            String date =
                    r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE);
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
        }
    }

    /* ============================================================
     * Internal holder
     * ============================================================ */

    private record BaseMeta(String displayName, long sizeBytes, long momentMillis) {}
}
