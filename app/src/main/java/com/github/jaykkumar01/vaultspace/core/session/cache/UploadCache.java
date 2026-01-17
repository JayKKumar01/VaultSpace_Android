package com.github.jaykkumar01.vaultspace.core.session.cache;

import androidx.annotation.Nullable;

import com.github.jaykkumar01.vaultspace.core.upload.UploadSnapshot;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * UploadCache
 *
 * Session-scoped cache holding per-album upload snapshots
 * and global stop context.
 *
 * Info-only. No execution. No callbacks.
 */
public final class UploadCache extends VaultCache {

    /* ==========================================================
     * Stop Reason
     * ========================================================== */

    public enum StopReason {
        NONE,
        USER,
        SYSTEM
    }

    /* ==========================================================
     * Storage
     * ========================================================== */

    private final Map<String, UploadSnapshot> snapshotsByAlbumId =
            new LinkedHashMap<>();

    private StopReason stopReason = StopReason.NONE;

    /* ==========================================================
     * Read APIs (O(1))
     * ========================================================== */

    @Nullable
    public UploadSnapshot getSnapshot(String albumId) {
        if (!isInitialized() || albumId == null) return null;
        return snapshotsByAlbumId.get(albumId);
    }

    /**
     * Read-only view of all snapshots.
     * Used by notification aggregation.
     */
    public Map<String, UploadSnapshot> getAllSnapshots() {
        if (!isInitialized()) return Collections.emptyMap();
        return Collections.unmodifiableMap(snapshotsByAlbumId);
    }

    public boolean hasAnyActiveUploads() {
        if (!isInitialized()) return false;

        for (UploadSnapshot s : snapshotsByAlbumId.values()) {
            if (s.isInProgress()) return true;
        }
        return false;
    }

    public boolean hasFailures() {
        if (!isInitialized()) return false;

        for (UploadSnapshot s : snapshotsByAlbumId.values()) {
            if (s.hasFailures()) return true;
        }
        return false;
    }

    public boolean albumNeedsAttention(String albumId) {
        UploadSnapshot s = getSnapshot(albumId);
        return s != null && s.hasFailures();
    }

    public int getAlbumsNeedingAttentionCount() {
        if (!isInitialized()) return 0;

        int count = 0;
        for (UploadSnapshot s : snapshotsByAlbumId.values()) {
            if (s.hasFailures()) count++;
        }
        return count;
    }

    public StopReason getStopReason() {
        return stopReason;
    }

    /* ==========================================================
     * Write APIs (UploadManager only)
     * ========================================================== */

    public void putSnapshot(UploadSnapshot snapshot) {
        if (snapshot == null) return;

        if (!isInitialized()) {
            markInitialized();
        }

        snapshotsByAlbumId.put(snapshot.groupId, snapshot);
    }

    public void removeSnapshot(String albumId) {
        if (!isInitialized() || albumId == null) return;
        snapshotsByAlbumId.remove(albumId);
    }

    public void markStopped(StopReason reason) {
        stopReason = reason == null ? StopReason.NONE : reason;
    }

    public void clearStopReason() {
        stopReason = StopReason.NONE;
    }

    /* ==========================================================
     * VaultCache hook
     * ========================================================== */

    @Override
    protected void onClear() {
        snapshotsByAlbumId.clear();
        stopReason = StopReason.NONE;
    }
}
