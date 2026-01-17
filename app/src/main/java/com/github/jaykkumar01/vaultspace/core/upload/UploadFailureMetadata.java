package com.github.jaykkumar01.vaultspace.core.upload;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.jaykkumar01.vaultspace.models.base.UploadType;

public final class UploadFailureMetadata {

    @NonNull final String groupId;
    @NonNull final String displayName;
    @NonNull final UploadType type;
    @Nullable final String thumbnailPath;

    UploadFailureMetadata(
            @NonNull String groupId,
            @NonNull String displayName,
            @NonNull UploadType type,
            @Nullable String thumbnailPath
    ) {
        this.groupId = groupId;
        this.displayName = displayName;
        this.type = type;
        this.thumbnailPath = thumbnailPath;
    }
}
