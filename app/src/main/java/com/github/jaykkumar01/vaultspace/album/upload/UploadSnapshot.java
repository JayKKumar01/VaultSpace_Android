package com.github.jaykkumar01.vaultspace.album.upload;

import androidx.annotation.NonNull;

/**
 * UploadSnapshot
 *
 * Immutable, per-album quantitative snapshot of upload progress.
 *
 * Responsibilities:
 * - Hold counts only
 * - Represent ONE album
 *
 * Non-responsibilities:
 * - State (uploading / stopped / completed)
 * - UI decisions
 * - Retry logic
 * - Lifecycle meaning
 */
public final class UploadSnapshot {

    public final String albumId;
    public final String albumName;

    public final int photos;
    public final int videos;
    public final int total;

    public final int uploaded;
    public final int failed;

    public UploadSnapshot(
            @NonNull String albumId,
            @NonNull String albumName,
            int photos,
            int videos,
            int uploaded,
            int failed
    ) {
        this.albumId = albumId;
        this.albumName = albumName;
        this.photos = photos;
        this.videos = videos;
        this.total = photos + videos;
        this.uploaded = uploaded;
        this.failed = failed;
    }

    /* ---------------- Derived helpers (optional, safe) ---------------- */

    public boolean isComplete() {
        return uploaded + failed == total;
    }

    public boolean hasFailures() {
        return failed > 0;
    }

    public boolean isInProgress() {
        return uploaded + failed < total;
    }

    @NonNull
    @Override
    public String toString() {
        return "UploadSnapshot{" +
                "albumId='" + albumId + '\'' +
                ", photos=" + photos +
                ", videos=" + videos +
                ", total=" + total +
                ", uploaded=" + uploaded +
                ", failed=" + failed +
                '}';
    }
}
