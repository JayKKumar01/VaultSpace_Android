package com.github.jaykkumar01.vaultspace.album.helper;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DriveThumbnailResolver {

    private static final String TAG = "VaultSpace:ThumbResolver";
    private static final String SEP = "|";
    private static final String THUMB_PREFIX = "thumb_";
    private static final String THUMB_EXT = ".jpg";

    private final Context appContext;
    private final ExecutorService executor;

    public DriveThumbnailResolver(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
    }

    /* ==========================================================
     * STEP 1: metadata-only (used by AlbumMediaFetcher)
     * ========================================================== */

    /**
     * Returns thumbnail reference:
     *
     * image/*  -> thumbnailLink
     * video/*  -> email|thumbFileId
     * others   -> null
     */
    @Nullable
    public String resolve(
            @NonNull String accountEmail,
            @NonNull File file
    ) {
        String mime = file.getMimeType();
        if (mime == null) return null;

        // 🖼 IMAGE
        if (mime.startsWith("image/")) {
            return file.getThumbnailLink();
        }

        // 🎥 VIDEO
        if (mime.startsWith("video/")) {
            Map<String, String> props = file.getAppProperties();
            if (props == null) return null;

            String thumbId = props.get("thumb");
            if (thumbId == null) return null;

            return accountEmail + SEP + thumbId;
        }

        return null;
    }

    /* ==========================================================
     * STEP 2: reference → cached path
     * ========================================================== */

    /**
     * Synchronous resolution.
     * Safe to call from background thread.
     */
    @Nullable
    public String resolveCachedPath(@Nullable String compositeRef) {
        if (compositeRef == null) return null;

        int idx = compositeRef.indexOf(SEP);
        if (idx <= 0) {
            // image thumbnailLink (URL)
            return compositeRef;
        }

        String email = compositeRef.substring(0, idx);
        String thumbFileId = compositeRef.substring(idx + 1);

        java.io.File out = new java.io.File(
                appContext.getCacheDir(),
                THUMB_PREFIX + thumbFileId + THUMB_EXT
        );

        if (out.exists()) return out.getAbsolutePath();

        Drive drive = DriveClientProvider.forAccount(appContext, email);
        return fetchAndCache(drive, thumbFileId, out);
    }

    /**
     * Asynchronous resolution.
     * This is what you’ll use later from adapter / prefetch logic.
     */
    public void resolveCachedPathAsync(
            @Nullable String compositeRef,
            @NonNull Callback callback
    ) {
        executor.execute(() -> {
            String result = resolveCachedPath(compositeRef);
            callback.onResult(result);
        });
    }

    /* ==========================================================
     * Internal helpers
     * ========================================================== */

    private String fetchAndCache(
            @NonNull Drive drive,
            @NonNull String thumbFileId,
            @NonNull java.io.File out
    ) {
        try (OutputStream os = new FileOutputStream(out)) {
            drive.files()
                    .get(thumbFileId)
                    .executeMediaAndDownloadTo(os);
            return out.getAbsolutePath();
        } catch (Exception e) {
            Log.w(TAG, "Thumbnail fetch failed id=" + thumbFileId, e);
            return null;
        }
    }

    /* ==========================================================
     * Lifecycle
     * ========================================================== */

    public void release() {
        executor.shutdown();
    }

    /* ==========================================================
     * Callback
     * ========================================================== */

    public interface Callback {
        void onResult(@Nullable String pathOrUrl);
    }
}
