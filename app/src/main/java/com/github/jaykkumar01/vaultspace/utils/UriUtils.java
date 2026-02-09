package com.github.jaykkumar01.vaultspace.utils;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;

import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

public final class UriUtils {
    private static final String TAG = "VaultSpace:UriUtils";


    private UriUtils() {
    }

    /* =========================
     * Public API
     * ========================= */

    public static boolean isPermissionRevoked(@NonNull Context ctx, @NonNull Uri uri) {
        try (Cursor c = ctx.getContentResolver().query(
                uri, new String[]{OpenableColumns.DISPLAY_NAME},
                null, null, null
        )) {
            return c == null || !c.moveToFirst();
        } catch (Exception e) {
            return true;
        }
    }

    @NonNull
    public static UploadSelection resolve(@NonNull Context ctx, @NonNull String groupId, @NonNull Uri uri) {
        ContentResolver cr = ctx.getContentResolver();

        BaseMeta base = readBaseMeta(cr, uri);
        String mime = cr.getType(uri);
        UploadType type = mime != null ? UploadType.fromMime(mime) : UploadType.FILE;

        long originMoment = -1;
        long momentMillis = -1;

        float aspectRatio = 1f;
        int rotation = 0;
        long durationMillis = 0;


        /* ---------- Media-aware extraction ---------- */

        if (type == UploadType.PHOTO) {
            try (InputStream in = cr.openInputStream(uri)) {
                if (in != null) {
                    ExifInterface exif = new ExifInterface(in);

                    originMoment = readExifOrigin(exif);

                    int exifOrientation =
                            exif.getAttributeInt(
                                    ExifInterface.TAG_ORIENTATION,
                                    ExifInterface.ORIENTATION_NORMAL
                            );

                    rotation = exifRotationToDegrees(exifOrientation);

                    int w = readExifWidth(exif);
                    int h = readExifHeight(exif);
                    aspectRatio = computeAspectRatio(w, h, rotation);

                    // 🔍 ALWAYS LOG (minimal, diagnostic)
                    Log.d(TAG,
                            "PHOTO geometry | uri=" + uri +
                                    " exifOri=" + exifOrientation +
                                    " rot=" + rotation +
                                    " w=" + w + " h=" + h +
                                    " ar=" + aspectRatio);

                    if (base.modifiedMillis <= 0 && originMoment > 0) {
                        momentMillis = originMoment;
                    }
                }
            } catch (Exception ignored) {
            }
        } else if (type == UploadType.VIDEO) {
            try (MediaMetadataRetriever r = new MediaMetadataRetriever()) {
                r.setDataSource(ctx, uri);

                originMoment = readVideoOrigin(r);

                String rotStr = r.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
                );
                rotation = parseRotation(rotStr);

                int w = parseInt(
                        r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                );
                int h = parseInt(
                        r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                );

                aspectRatio = computeAspectRatio(w, h, rotation);

                // ✅ duration (ONE metadata call, zero extra cost)
                durationMillis = parseLong(
                        r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                );

                Log.d(TAG,
                        "VIDEO meta | uri=" + uri +
                                " rot=" + rotation +
                                " w=" + w + " h=" + h +
                                " ar=" + aspectRatio +
                                " durMs=" + durationMillis);

                if (base.modifiedMillis <= 0) {
                    momentMillis = readVideoDateTaken(cr, uri);
                    if (momentMillis <= 0) momentMillis = originMoment;
                }
            } catch (Exception ignored) {
            }
        }



        /* ---------- Final moment resolution ---------- */

        if (base.modifiedMillis > 0) {
            momentMillis = base.modifiedMillis;
        }

        if (originMoment > 0 && momentMillis > 0) {
            momentMillis = Math.max(momentMillis, originMoment);
        }

        if (momentMillis <= 0) {
            momentMillis = System.currentTimeMillis();
        }

        /* ---------- Geometry safety ---------- */

        aspectRatio = sanitizeAspectRatio(aspectRatio);
        rotation = sanitizeRotation(rotation);

        /* ---------- Thumbnail ---------- */

        String thumb = null;
        if (type == UploadType.PHOTO || type == UploadType.VIDEO) {
            File dir = new File(ctx.getCacheDir(), "thumbs");
            @SuppressLint("ResultOfMethodCallIgnored")
            boolean ignored = dir.exists() || dir.mkdirs();
            thumb = ThumbnailGenerator.generate(ctx, uri, type, dir);
        }

        String id = UUID.randomUUID().toString();
        return new UploadSelection(
                id,
                groupId,
                uri,
                mime,
                base.displayName,
                base.sizeBytes,
                originMoment,
                momentMillis,
                aspectRatio,
                rotation,
                durationMillis,
                thumb
        );

    }

    /* =========================
     * Base metadata (single query)
     * ========================= */

    private static BaseMeta readBaseMeta(ContentResolver cr, Uri uri) {
        String name = "unknown";
        long size = -1, modified = -1;
        try (Cursor c = cr.query(
                uri,
                new String[]{
                        OpenableColumns.DISPLAY_NAME,
                        OpenableColumns.SIZE,
                        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                        MediaStore.MediaColumns.DATE_MODIFIED
                },
                null, null, null
        )) {
            if (c != null && c.moveToFirst()) {
                int n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int s = c.getColumnIndex(OpenableColumns.SIZE);
                int d = c.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED);
                int m = c.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED);

                if (n != -1) name = c.getString(n);
                if (s != -1) size = c.getLong(s);

                if (d != -1) {
                    long t = c.getLong(d);
                    if (t > 0) modified = t;
                }

                if (modified <= 0 && m != -1) {
                    long sec = c.getLong(m);
                    if (sec > 0) modified = sec * 1000L;
                }
            }
        } catch (Exception ignored) {
        }
        return new BaseMeta(name, size, modified);
    }

    /* ==========================================================
     * Geometry helpers (PURE, metadata-only)
     * ========================================================== */

    private static float computeAspectRatio(int w, int h, int rotation){
        if(w <= 0 || h <= 0){
            Log.d(TAG,
                    "AR compute SKIP | invalid dims w=" + w +
                            " h=" + h +
                            " rot=" + rotation);
            return 1f;
        }

        int r = Math.abs(rotation);
        boolean swapped = (r == 90 || r == 270);

        float ar = swapped
                ? (h / (float) w)
                : (w / (float) h);

        Log.d(TAG,
                "AR compute | w=" + w +
                        " h=" + h +
                        " rot=" + rotation +
                        " absRot=" + r +
                        " mirror=" + (rotation < 0) +
                        " axisSwap=" + swapped +
                        " ar=" + ar);

        return ar;
    }



    private static float sanitizeAspectRatio(float ar) {
        return (ar <= 0f || Float.isNaN(ar) || Float.isInfinite(ar)) ? 1f : ar;
    }

    private static int sanitizeRotation(int r){
        int a = Math.abs(r);
        return (a == 0 || a == 90 || a == 180 || a == 270) ? r : 0;
    }


    private static int exifRotationToDegrees(int o) {
        return switch (o) {

            case ExifInterface.ORIENTATION_ROTATE_90 -> 90;
            case ExifInterface.ORIENTATION_ROTATE_180 -> 180;
            case ExifInterface.ORIENTATION_ROTATE_270 -> 270;

            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> -0;   // mirror, no rotation
            case ExifInterface.ORIENTATION_FLIP_VERTICAL -> -180;  // mirror + 180

            case ExifInterface.ORIENTATION_TRANSPOSE -> -270;      // mirror + 90
            case ExifInterface.ORIENTATION_TRANSVERSE -> -90;      // mirror + 270

            default -> 0;
        };
    }


    private static int readExifWidth(ExifInterface exif) {
        int v = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, -1);
        return v > 0 ? v : exif.getAttributeInt(
                ExifInterface.TAG_PIXEL_X_DIMENSION, -1
        );
    }

    private static int readExifHeight(ExifInterface exif) {
        int v = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, -1);
        return v > 0 ? v : exif.getAttributeInt(
                ExifInterface.TAG_PIXEL_Y_DIMENSION, -1
        );
    }

    /* =========================
     * Origin helpers
     * ========================= */

    private static long readExifOrigin(ExifInterface exif) {
        String dt = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);
        if (dt == null) dt = exif.getAttribute(ExifInterface.TAG_DATETIME);
        return dt != null ? parseExif(dt) : -1;
    }

    private static long readVideoOrigin(MediaMetadataRetriever r) {
        String date = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE);
        if (date == null) return -1;
        try {
            date = date.replace("-", "").replace(":", "");
            SimpleDateFormat f = new SimpleDateFormat(
                    "yyyyMMdd'T'HHmmss.SSS'Z'", Locale.US
            );
            f.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d = f.parse(date);
            return d != null ? d.getTime() : -1;
        } catch (Exception ignored) {
            return -1;
        }
    }

    /* =========================
     * Moment helpers
     * ========================= */

    private static long readVideoDateTaken(ContentResolver cr, Uri uri) {
        try (Cursor c = cr.query(
                uri, new String[]{MediaStore.Video.VideoColumns.DATE_TAKEN},
                null, null, null
        )) {
            if (c != null && c.moveToFirst()) {
                long t = c.getLong(0);
                if (t > 0) return t;
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    private static long parseExif(String dt) {
        try {
            SimpleDateFormat f =
                    new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US);
            Date d = f.parse(dt);
            return d != null ? d.getTime() : -1;
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static int parseRotation(String v) {
        try {
            return v != null ? Integer.parseInt(v) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int parseInt(String v) {
        try {
            return v != null ? Integer.parseInt(v) : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private static long parseLong(String v) {
        try {
            return v != null ? Long.parseLong(v) : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }


    /* =========================
     * Internal holder
     * ========================= */

    private record BaseMeta(String displayName, long sizeBytes, long modifiedMillis) {
    }
}
