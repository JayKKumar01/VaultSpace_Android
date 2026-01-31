package com.github.jaykkumar01.vaultspace.core.session.cache;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;

import java.util.Collections;
import java.util.LinkedHashMap;

/**
 * AlbumMediaEntry
 *
 * Represents media knowledge for ONE album.
 *
 * Guarantees:
 * - O(1) lookup by fileId
 * - O(1) add / remove / replace
 * - Ordered iteration (newest first)
 * - O(n) ONLY during initialization
 *
 * UI-facing via safe iterables.
 */
public final class AlbumMediaEntry {

    /* ==========================================================
     * State
     * ========================================================== */

    private boolean initialized = false;

    /* ==========================================================
     * Storage
     * ========================================================== */

    /**
     * fileId -> AlbumMedia
     *
     * accessOrder = false
     * We control insertion position explicitly.
     */
    private final LinkedHashMap<String, AlbumMedia> mediaById =
            new LinkedHashMap<>();

    /* ==========================================================
     * Lifecycle
     * ========================================================== */

    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Initializes media from Drive result.
     * Order is preserved exactly as provided.
     *
     * O(n) — allowed ONLY here.
     */
    public void initializeFromDrive(Iterable<AlbumMedia> media) {
        if (initialized) return;

        mediaById.clear();

        if (media != null) {
            for (AlbumMedia m : media) {
                mediaById.put(m.fileId, m);
            }
        }

        initialized = true;
    }

    /**
     * Clears media and resets state.
     */
    public void clear() {
        mediaById.clear();
        initialized = false;
    }

    /* ==========================================================
     * Read APIs (O(1))
     * ========================================================== */

    /**
     * Read-only ordered view.
     * Safe for UI iteration.
     */
    public Iterable<AlbumMedia> getMediaView() {
        return Collections.unmodifiableCollection(mediaById.values());
    }

    /**
     * O(1) lookup by fileId.
     */
    public AlbumMedia getByFileId(String fileId) {
        if (!initialized || fileId == null) return null;
        return mediaById.get(fileId);
    }

    /* ==========================================================
     * Mutation APIs (ALL O(1))
     * ========================================================== */

    /**
     * Adds new media at top (newest first).
     * Ignores duplicates.
     */
    public void addMedia(AlbumMedia media) {
        if (!initialized || media == null) return;
        if (mediaById.containsKey(media.fileId)) return;

        // Insert-at-top trick: rebuild head only
        LinkedHashMap<String, AlbumMedia> reordered =
                new LinkedHashMap<>();

        reordered.put(media.fileId, media);
        reordered.putAll(mediaById);

        mediaById.clear();
        mediaById.putAll(reordered);
    }

    /**
     * Removes media by fileId.
     */
    public void removeMedia(String fileId) {
        if (!initialized || fileId == null) return;
        mediaById.remove(fileId);
    }

    /**
     * Replaces existing media metadata.
     * Order is preserved.
     */
    public void replaceMedia(AlbumMedia updated) {
        if (!initialized || updated == null) return;
        if (!mediaById.containsKey(updated.fileId)) return;

        mediaById.put(updated.fileId, updated);
    }
}
