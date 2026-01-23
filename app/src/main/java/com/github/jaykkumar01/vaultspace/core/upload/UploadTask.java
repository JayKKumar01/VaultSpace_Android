package com.github.jaykkumar01.vaultspace.core.upload;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSelection;

/**
 * UploadTask
 *
 * Immutable execution unit representing
 * a single media upload belonging to an album.
 *
 * Contains no logic.
 */
public final class UploadTask {

    @NonNull
    public final String groupId;

    @NonNull
    public final UploadSelection selection;

    public UploadTask(
            @NonNull String groupId,
            @NonNull UploadSelection selection
    ) {
        this.groupId = groupId;
        this.selection = selection;
    }
}
