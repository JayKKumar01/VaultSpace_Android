package com.github.jaykkumar01.vaultspace.album.helper;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.jaykkumar01.vaultspace.album.AlbumMedia;
import com.github.jaykkumar01.vaultspace.album.MediaGeometry;
import com.github.jaykkumar01.vaultspace.album.Moments;
import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class DriveResolver {

    private static final String TAG = "VaultSpace:ThumbResolver";
    private static final String THUMB_PREFIX = "thumb_";
    private static final String THUMB_EXT = ".jpg";

    private final Context appContext;
    private final ExecutorService executor;
    private final Drive primaryDrive;

    public DriveResolver(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.executor = Executors.newFixedThreadPool(3);
        this.primaryDrive = DriveClientProvider.getPrimaryDrive(context);
    }

    /* ==========================================================
     * STEP 1: metadata-only extraction (NO IO)
     * ========================================================== */

    @Nullable
    public String resolve(@NonNull File file) {
        String mime = file.getMimeType();
        if (mime == null) return null;

        Map<String, String> props = file.getAppProperties();
        if (props != null && props.containsKey("thumb"))
            return props.get("thumb");

        if (mime.startsWith("image/") || mime.startsWith("video/"))
            return file.getThumbnailLink();

        return null;
    }

    @NonNull
    public Moments resolveMoments(@NonNull File file) {

        long created = file.getCreatedTime() != null
                ? file.getCreatedTime().getValue()
                : -1;

        long modified = file.getModifiedTime() != null
                ? file.getModifiedTime().getValue()
                : created;

        String source = null;
        if (file.getAppProperties() != null)
            source = file.getAppProperties().get("vs_created_source");

        boolean vsOrigin = "origin".equals(source);

        long originMoment = vsOrigin ? created : -1;

        return new Moments(originMoment, modified, vsOrigin);
    }

    @NonNull
    public MediaGeometry resolveGeometry(@NonNull File file) {

        float aspectRatio = 1f;
        int rotation = 0;

        Map<String, String> props = file.getAppProperties();
        if (props != null) {
            try {
                String ar = props.get("vs_aspect_ratio");
                if (ar != null) aspectRatio = Float.parseFloat(ar);
            } catch (Exception ignored) {
            }

            try {
                String rot = props.get("vs_rotation");
                if (rot != null) rotation = Integer.parseInt(rot);
            } catch (Exception ignored) {
            }
        }

        // Hard safety clamps (important for layout stability)
        if (aspectRatio <= 0f || Float.isNaN(aspectRatio) || Float.isInfinite(aspectRatio))
            aspectRatio = 1f;

        if (rotation != 0 && rotation != 90 && rotation != 180 && rotation != 270)
            rotation = 0;

        return new MediaGeometry(aspectRatio, rotation);
    }



    /* ==========================================================
     * STEP 2: async resolution entry
     * ========================================================== */

    public void resolveAsync(
            @NonNull AlbumMedia media,
            @NonNull Consumer<String> consumer
    ) {
        executor.execute(() -> consumer.accept(resolveOnce(media)));
    }

    /* ==========================================================
     * Core resolution logic (single pass, no retries)
     * ========================================================== */

    @Nullable
    private String resolveOnce(@NonNull AlbumMedia media) {
        if (media.mimeType == null) return null;

        String ref = media.thumbnailLink;
        if (ref == null || ref.isEmpty()) return null;

        if (ref.startsWith("http"))
            return resolveHttp(ref);

        return resolveDriveThumb(ref);
    }


    /* ==========================================================
     * Drive thumbnail handling (explicit thumbnails)
     * ========================================================== */

    private String resolveDriveThumb(@NonNull String thumbFileId) {
        java.io.File out = new java.io.File(
                appContext.getCacheDir(),
                THUMB_PREFIX + thumbFileId + THUMB_EXT
        );

        if (out.exists()) return out.getAbsolutePath();
        return fetchAndCache(primaryDrive, thumbFileId, out);
    }

    private String fetchAndCache(
            @NonNull Drive drive,
            @NonNull String thumbFileId,
            @NonNull java.io.File out
    ) {
        try (OutputStream os = new FileOutputStream(out)) {
            drive.files().get(thumbFileId).executeMediaAndDownloadTo(os);
            return out.getAbsolutePath();
        } catch (Exception e) {
            Log.w(TAG, "Drive thumbnail fetch failed id=" + thumbFileId, e);
            out.delete();
            return null;
        }
    }

    /* ==========================================================
     * HTTP thumbnail handling (fallback only)
     * ========================================================== */

    private String resolveHttp(@NonNull String url) {
        java.io.File out = new java.io.File(
                appContext.getCacheDir(),
                THUMB_PREFIX + sha1(url) + THUMB_EXT
        );

        if (out.exists()) return out.getAbsolutePath();
        return downloadHttp(url, out);
    }

    private String downloadHttp(String url, java.io.File out) {
        HttpURLConnection conn = null;
        try (FileOutputStream fos = new FileOutputStream(out)) {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.connect();

            try (InputStream in = conn.getInputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) fos.write(buf, 0, n);
            }
            return out.getAbsolutePath();
        } catch (Exception e) {
            Log.w(TAG, "HTTP thumbnail fetch failed", e);
            out.delete();
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /* ==========================================================
     * Utils
     * ========================================================== */

    private static String sha1(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] b = md.digest(s.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(s.hashCode());
        }
    }

    /* ==========================================================
     * Lifecycle
     * ========================================================== */

    public void release() {
        executor.shutdown();
    }
}
