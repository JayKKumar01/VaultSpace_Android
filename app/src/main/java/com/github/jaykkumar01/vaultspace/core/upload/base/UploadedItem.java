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

    // 🔑 layout-critical (NEW)
    public final float aspectRatio;  // width / height (already rotation-corrected)
    public final int rotation;       // 0, 90, 180, 270

    @Nullable public final String thumbnailLink;

    public UploadedItem(
            @NonNull String fileId,
            @NonNull String name,
            @NonNull String mimeType,
            long sizeBytes,
            long originMoment,
            long momentMillis,
            boolean vsOrigin,
            float aspectRatio,
            int rotation,
            @Nullable String thumbnailLink
    ) {
        this.fileId = fileId;
        this.name = name;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.originMoment = originMoment;
        this.momentMillis = momentMillis;
        this.vsOrigin = vsOrigin;
        this.aspectRatio = aspectRatio;
        this.rotation = rotation;
        this.thumbnailLink = thumbnailLink;
    }

    @NonNull
    public UploadType getType() {
        return UploadType.fromMime(mimeType);
    }
}
