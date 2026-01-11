package com.github.jaykkumar01.vaultspace.core.session.cache;

import com.github.jaykkumar01.vaultspace.album.AlbumMedia;

import java.util.HashMap;
import java.util.Map;

/**
 * AlbumMediaCache
 *
 * Session-scoped cache for album media.
 * Routes media access by albumId.
 *
 * Responsibilities:
 * - Manage per-album media entries
 * - Enforce session lifecycle
 *
 * Does NOT:
 * - Talk to Drive
 * - Decide fetch timing
 * - Expose collections
 */
public final class AlbumMediaCache extends VaultCache {

    /* ==========================================================
     * Storage
     * ========================================================== */

    private final Map<String, AlbumMediaEntry> entriesByAlbumId =
            new HashMap<>();

    /* ==========================================================
     * Album routing
     * ========================================================== */

    /**
     * Returns the media entry for an album.
     * Creates a new empty entry if none exists yet.
     *
     * Safe to call anytime.
     */
    public AlbumMediaEntry getOrCreateEntry(String albumId) {
        if (albumId == null) {
            throw new IllegalArgumentException("albumId == null");
        }

        AlbumMediaEntry entry = entriesByAlbumId.get(albumId);
        if (entry == null) {
            entry = new AlbumMediaEntry();
            entriesByAlbumId.put(albumId, entry);
        }
        return entry;
    }

    /**
     * Invalidates media cache for a single album.
     * UI will refetch if needed.
     */
    public void invalidateAlbum(String albumId) {
        if (albumId == null) return;

        AlbumMediaEntry entry = entriesByAlbumId.get(albumId);
        if (entry != null) {
            entry.clear();
        }
    }

    /* ==========================================================
     * VaultCache hook
     * ========================================================== */

    @Override
    protected void onClear() {
        entriesByAlbumId.clear();
    }
}
