package com.github.jaykkumar01.vaultspace.core.session;

/**
 * In-memory, session-scoped cache.
 * Cleared when user logs out or app process dies.
 */
public class VaultSessionCache {

    /* ---------------- Albums ---------------- */

    // null = unknown, true/false = cached
    private Boolean hasAlbums;

    public boolean hasAlbumsCached() {
        return hasAlbums != null;
    }

    public boolean getHasAlbums() {
        return hasAlbums != null && hasAlbums;
    }

    public void setHasAlbums(boolean value) {
        this.hasAlbums = value;
    }

    public void invalidateAlbums() {
        this.hasAlbums = null;
    }

    /* ---------------- Files (future) ---------------- */
    // Boolean hasFiles;
    // List<FileItem> cachedFiles;

    /* ---------------- Clear ---------------- */

    public void clear() {
        hasAlbums = null;
        // clear files cache later
    }
}
