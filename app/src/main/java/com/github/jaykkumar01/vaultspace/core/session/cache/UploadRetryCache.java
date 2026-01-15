package com.github.jaykkumar01.vaultspace.core.session.cache;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.models.MediaSelection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UploadRetryCache
 *
 * Session-scoped cache holding retry intent per album.
 *
 * Stores MediaSelection objects only.
 * No execution. No state derivation. No UI logic.
 */
public final class UploadRetryCache extends VaultCache {

    /* ==========================================================
     * Storage
     * ========================================================== */

    private final Map<String, List<MediaSelection>> retryByAlbumId =
            new HashMap<>();

    /* ==========================================================
     * Read APIs (O(1))
     * ========================================================== */

    public boolean hasRetry(@NonNull String albumId) {
        if (!isInitialized()) return false;

        List<MediaSelection> list = retryByAlbumId.get(albumId);
        return list != null && !list.isEmpty();
    }

    /**
     * Returns retry selections for an album.
     * Caller MUST treat result as immutable.
     */
    @NonNull
    public List<MediaSelection> getRetrySelections(@NonNull String albumId) {
        if (!isInitialized()) return Collections.emptyList();

        List<MediaSelection> list = retryByAlbumId.get(albumId);
        return list == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(list);
    }

    /**
     * Returns albumIds that currently have retryable items.
     * Used for album list warning badges.
     */
    @NonNull
    public Iterable<String> getAlbumsWithRetry() {
        if (!isInitialized()) return Collections.emptyList();
        return Collections.unmodifiableSet(retryByAlbumId.keySet());
    }

    /* ==========================================================
     * Write APIs (UploadManager only)
     * ========================================================== */

    public void addRetry(
            @NonNull String albumId,
            @NonNull MediaSelection selection
    ) {
        if (!isInitialized()) {
            markInitialized();
        }

        List<MediaSelection> list = retryByAlbumId.get(albumId);
        if (list == null) {
            list = new ArrayList<>();
            retryByAlbumId.put(albumId, list);
        }

        list.add(selection);
    }

    public void addRetryBatch(
            @NonNull String albumId,
            @NonNull List<MediaSelection> selections
    ) {
        if (selections.isEmpty()) return;

        if (!isInitialized()) {
            markInitialized();
        }

        List<MediaSelection> list = retryByAlbumId.get(albumId);
        if (list == null) {
            list = new ArrayList<>(selections.size());
            retryByAlbumId.put(albumId, list);
        }

        list.addAll(selections);
    }

    /**
     * Clears retry data for an album.
     * Called immediately when retry intent is sent.
     */
    public void clearRetry(@NonNull String albumId) {
        if (!isInitialized()) return;
        retryByAlbumId.remove(albumId);
    }

    /* ==========================================================
     * VaultCache hook
     * ========================================================== */

    @Override
    protected void onClear() {
        retryByAlbumId.clear();
    }
}
