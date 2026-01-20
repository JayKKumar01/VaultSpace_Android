package com.github.jaykkumar01.vaultspace.models.base;

import androidx.annotation.NonNull;

public final class UploadedItem {

    @NonNull public final String fileId;
    @NonNull public final String name;
    @NonNull public final String mimeType;
    public final long sizeBytes;
    public final long modifiedTime;
    public final String thumbnailLink;

    public UploadedItem(
            @NonNull String fileId,
            @NonNull String name,
            @NonNull String mimeType,
            long sizeBytes,
            long modifiedTime,
            String thumbnailLink
    ) {
        this.fileId = fileId;
        this.name = name;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.modifiedTime = modifiedTime;
        this.thumbnailLink = thumbnailLink;
    }

    @NonNull
    public UploadType getType() {
        return UploadType.fromMime(mimeType);
    }
}
