package com.github.jaykkumar01.vaultspace.core.upload.base;

import androidx.annotation.NonNull;

/**
 * UploadSnapshot
 *
 * Immutable, per-group quantitative snapshot of upload progress.
 *
 * Responsibilities:
 * - Hold counts only
 * - Represent ONE upload group (album, files, etc.)
 *
 * Non-responsibilities:
 * - Upload execution
 * - Retry logic
 * - Lifecycle decisions
 * - UI branching
 */
public final class UploadSnapshot {

    public final String groupId;
    public final String groupName;

    public final int photos;
    public final int videos;
    public final int others;   // ✅ NEW (files / non-media)

    public final int total;

    public final int uploaded;
    public final int failed;

    // Failures that cannot be retried (permission lost, etc.)
    // Mutable by design — incremented during upload execution
    public int nonRetryableFailed = 0;

    public UploadSnapshot(
            @NonNull String groupId,
            @NonNull String groupName,
            int photos,
            int videos,
            int others,
            int uploaded,
            int failed
    ) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.photos = photos;
        this.videos = videos;
        this.others = others;
        this.total = photos + videos + others;
        this.uploaded = uploaded;
        this.failed = failed;
    }

    /* ---------------- Derived helpers (pure) ---------------- */

    public boolean isComplete() {
        return uploaded + failed == total;
    }

    public boolean isInProgress() {
        return uploaded + failed < total;
    }

    public boolean hasFailures() {
        return failed > 0;
    }

    public boolean hasNonRetryableFailures() {
        return nonRetryableFailed > 0;
    }

    public boolean hasRetryableFailures() {
        return failed > nonRetryableFailed;
    }

    public boolean hasOnlyNonRetryableFailures() {
        return failed > 0 && failed == nonRetryableFailed;
    }

    @NonNull
    @Override
    public String toString() {
        return "UploadSnapshot{" +
                "groupId='" + groupId + '\'' +
                ", photos=" + photos +
                ", videos=" + videos +
                ", others=" + others +
                ", total=" + total +
                ", uploaded=" + uploaded +
                ", failed=" + failed +
                ", nonRetryableFailed=" + nonRetryableFailed +
                '}';
    }
}
