package com.github.jaykkumar01.vaultspace.core.upload;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.models.MediaSelection;

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
    final String albumId;

    @NonNull
    final MediaSelection selection;

    UploadTask(
            @NonNull String albumId,
            @NonNull MediaSelection selection
    ) {
        this.albumId = albumId;
        this.selection = selection;
    }
}
