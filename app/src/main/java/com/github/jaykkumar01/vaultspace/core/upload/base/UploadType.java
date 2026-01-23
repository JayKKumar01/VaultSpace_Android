package com.github.jaykkumar01.vaultspace.core.upload.base;

import androidx.annotation.NonNull;

public enum UploadType {
    PHOTO,
    VIDEO,
    FILE;

    @NonNull
    public static UploadType fromMime(@NonNull String mimeType) {
        if (mimeType.startsWith("image/")) return PHOTO;
        if (mimeType.startsWith("video/")) return VIDEO;
        return FILE;
    }
}
