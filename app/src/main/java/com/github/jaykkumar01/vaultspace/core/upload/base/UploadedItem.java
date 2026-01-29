package com.github.jaykkumar01.vaultspace.core.upload.base;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class UploadedItem {

    @NonNull public final String fileId;
    @NonNull public final String name;
    @NonNull public final String mimeType;

    public final long sizeBytes;

    public final long originMoment;
    public final long momentMillis;

    public final boolean vsOrigin;   // true = real origin, false = promoted moment

    @Nullable public final String thumbnailLink;

    public UploadedItem(
            @NonNull String fileId,
            @NonNull String name,
            @NonNull String mimeType,
            long sizeBytes,
            long originMoment,
            long momentMillis,
            boolean vsOrigin,
            @Nullable String thumbnailLink
    ) {
        this.fileId = fileId;
        this.name = name;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.originMoment = originMoment;
        this.momentMillis = momentMillis;
        this.vsOrigin = vsOrigin;
        this.thumbnailLink = thumbnailLink;
    }

    @NonNull
    public UploadType getType() {
        return UploadType.fromMime(mimeType);
    }
}
