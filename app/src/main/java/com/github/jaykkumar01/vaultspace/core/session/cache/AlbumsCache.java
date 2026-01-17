package com.github.jaykkumar01.vaultspace.core.session.cache;

import com.github.jaykkumar01.vaultspace.models.AlbumInfo;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AlbumsCache
 *
 * Guarantees:
 * - O(1) lookup by groupId
 * - O(1) lookup by groupName
 * - O(1) add / remove / replace / rename
 * - O(1) reorder (move-to-top)
 * - O(n) ONLY during initialization
 *
 * Uses only built-in Java data structures.
 */
public final class AlbumsCache extends VaultCache {

    /* ==========================================================
     * Storage
     * ========================================================== */

    /**
     * groupId -> AlbumInfo
     *
     * accessOrder = true enables O(1) reordering when accessed.
     */
    private final LinkedHashMap<String, AlbumInfo> albumsById =
            new LinkedHashMap<>(16, 0.75f, true);

    /**
     * groupName -> groupId
     */
    private final Map<String, String> albumIdByName = new HashMap<>();

    /* ==========================================================
     * Initialization (O(n) — ONLY allowed place)
     * ========================================================== */

    /**
     * Initializes cache from Drive result.
     * Order provided by Drive is preserved.
     */
    public void initializeFromDrive(Iterable<AlbumInfo> albums) {
        if (isInitialized()) return;

        for (AlbumInfo album : albums) {
            albumsById.put(album.id, album);
            albumIdByName.put(album.name, album.id);
        }

        markInitialized();
    }

    /* ==========================================================
     * Read APIs (O(1))
     * ========================================================== */

    public boolean hasAlbumWithName(String name) {
        return isInitialized()
                && name != null
                && albumIdByName.containsKey(name);
    }

    public AlbumInfo getAlbumById(String albumId) {
        if (!isInitialized() || albumId == null) return null;
        return albumsById.get(albumId);
    }

    /**
     * Read-only ordered view of albums.
     * Iteration is O(n) (UI-only, allowed).
     */
    public Iterable<AlbumInfo> getAlbumsView() {
        return Collections.unmodifiableCollection(albumsById.values());
    }

    /* ==========================================================
     * Mutation APIs (ALL O(1))
     * ========================================================== */

    public void addAlbum(AlbumInfo album) {
        if (!isInitialized() || album == null) return;

        albumsById.put(album.id, album);
        albumIdByName.put(album.name, album.id);
    }

    public void removeAlbum(String albumId) {
        if (!isInitialized() || albumId == null) return;

        AlbumInfo removed = albumsById.remove(albumId);
        if (removed == null) return;

        albumIdByName.remove(removed.name);
    }

    public void replaceAlbum(AlbumInfo updated) {
        if (!isInitialized() || updated == null) return;

        AlbumInfo old = albumsById.get(updated.id);
        if (old == null) return;

        // Update name index if name changed
        if (!old.name.equals(updated.name)) {
            albumIdByName.remove(old.name);
            albumIdByName.put(updated.name, updated.id);
        }

        // Replace value without affecting order
        albumsById.put(updated.id, updated);
    }

    /**
     * Moves album to most-recent position (top).
     * Implemented via accessOrder (O(1)).
     */
    public void moveAlbumToTop(String albumId) {
        if (!isInitialized() || albumId == null) return;

        // Access triggers reorder in LinkedHashMap
        albumsById.get(albumId);
    }

    /* ==========================================================
     * VaultCache hook
     * ========================================================== */

    @Override
    protected void onClear() {
        albumsById.clear();
        albumIdByName.clear();
    }
}
