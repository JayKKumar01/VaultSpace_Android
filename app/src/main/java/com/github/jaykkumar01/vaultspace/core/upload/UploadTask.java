package com.github.jaykkumar01.vaultspace.core.upload;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.models.base.UploadSelection;

/**
 * UploadTask
 *
 * Immutable execution unit representing
 * a single media upload belonging to an album.
 *
 * Contains no logic.
 */
final class UploadTask {

    @NonNull
    final String groupId;

    @NonNull
    final UploadSelection selection;

    UploadTask(
            @NonNull String groupId,
            @NonNull UploadSelection selection
    ) {
        this.groupId = groupId;
        this.selection = selection;
    }
}
