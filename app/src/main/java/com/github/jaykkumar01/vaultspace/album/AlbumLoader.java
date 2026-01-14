package com.github.jaykkumar01.vaultspace.album;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.core.session.cache.AlbumMediaEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AlbumLoader
 *
 * Responsibilities:
 * - Cache-first album loading
 * - Drive hydration when cache is cold
 * - Async execution
 *
 * Non-responsibilities:
 * - UI
 * - UiState
 * - View lifecycle
 */
public final class AlbumLoader {

    private static final String TAG = "VaultSpace:AlbumLoader";

    public interface Callback {
        void onDataLoaded();
        void onError(Exception e);
    }

    private final String albumId;
    private final AlbumMediaEntry mediaEntry;
    private final AlbumDriveHelper driveHelper;
    private final ExecutorService executor;

    private boolean released;

    public AlbumLoader(
            @NonNull Context context,
            @NonNull String albumId
    ) {
        this.albumId = albumId;

        this.mediaEntry =
                new UserSession(context)
                        .getVaultCache()
                        .albumMedia
                        .getOrCreateEntry(albumId);

        this.executor = Executors.newSingleThreadExecutor();
        this.driveHelper = new AlbumDriveHelper(context, albumId);

        Log.d(TAG, "Initialized for albumId=" + albumId);
    }

    /* ==========================================================
     * Public API
     * ========================================================== */

    /**
     * Cache-first load.
     * - If cache initialized → callback immediately
     * - Else → fetch from Drive, hydrate cache, callback
     */
    public void load(@NonNull Callback callback) {
        if (released) return;

        if (mediaEntry.isInitialized()) {
            callback.onDataLoaded();
            return;
        }

        driveHelper.fetchAlbumMedia(
                executor,
                new AlbumDriveHelper.FetchCallback() {

                    @Override
                    public void onResult(List<AlbumMedia> items) {
                        if (released) return;

                        mediaEntry.initializeFromDrive(items);
                        callback.onDataLoaded();
                    }

                    @Override
                    public void onError(Exception e) {
                        if (released) return;

                        Log.e(TAG, "load failed albumId=" + albumId, e);
                        callback.onError(e);
                    }
                }
        );
    }

    /**
     * Read-only access to cached media.
     * UI / Activity decides how to interpret it.
     */
    public List<AlbumMedia> getMedia() {
        List<AlbumMedia> list = new ArrayList<>();
        for (AlbumMedia media : mediaEntry.getMediaView()) {
            list.add(media);
        }
        return list;
    }


    /* ==========================================================
     * Future-safe helpers (not used yet)
     * ========================================================== */

    public void invalidate() {
        mediaEntry.clear();
    }

    public void refresh(@NonNull Callback callback) {
        invalidate();
        load(callback);
    }

    /* ==========================================================
     * Lifecycle
     * ========================================================== */

    public void release() {
        released = true;
        executor.shutdownNow();
        Log.d(TAG, "Released albumId=" + albumId);
    }
}
