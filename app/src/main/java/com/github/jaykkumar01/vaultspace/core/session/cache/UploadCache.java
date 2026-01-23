package com.github.jaykkumar01.vaultspace.core.session.cache;

import androidx.annotation.Nullable;

import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSnapshot;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * UploadCache
 *
 * Session-scoped cache holding per-group upload snapshots
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

    private final Map<String, UploadSnapshot> snapshotsByGroupId =
            new LinkedHashMap<>();

    private StopReason stopReason = StopReason.NONE;

    /* ==========================================================
     * Read APIs (O(1))
     * ========================================================== */

    @Nullable
    public UploadSnapshot getSnapshot(String groupId) {
        if (!isInitialized() || groupId == null) return null;
        return snapshotsByGroupId.get(groupId);
    }

    /**
     * Read-only view of all snapshots.
     * Used by notification aggregation.
     */
    public Map<String, UploadSnapshot> getAllSnapshots() {
        if (!isInitialized()) return Collections.emptyMap();
        return Collections.unmodifiableMap(snapshotsByGroupId);
    }

    public boolean hasAnyActiveUploads() {
        if (!isInitialized()) return false;

        for (UploadSnapshot s : snapshotsByGroupId.values()) {
            if (s.isInProgress()) return true;
        }
        return false;
    }

    public boolean hasFailures() {
        if (!isInitialized()) return false;

        for (UploadSnapshot s : snapshotsByGroupId.values()) {
            if (s.hasFailures()) return true;
        }
        return false;
    }

    public boolean groupNeedsAttention(String groupId) {
        UploadSnapshot s = getSnapshot(groupId);
        return s != null && s.hasFailures();
    }

    public int getGroupsNeedingAttentionCount() {
        if (!isInitialized()) return 0;

        int count = 0;
        for (UploadSnapshot s : snapshotsByGroupId.values()) {
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

        snapshotsByGroupId.put(snapshot.groupId, snapshot);
    }

    public void removeSnapshot(String groupId) {
        if (!isInitialized() || groupId == null) return;
        snapshotsByGroupId.remove(groupId);
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
        snapshotsByGroupId.clear();
        stopReason = StopReason.NONE;
    }
}
