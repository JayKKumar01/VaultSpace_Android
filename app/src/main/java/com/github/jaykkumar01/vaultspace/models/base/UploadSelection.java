package com.github.jaykkumar01.vaultspace.models.base;

import android.net.Uri;

import androidx.annotation.NonNull;

public abstract class UploadSelection {

    @NonNull
    public final Uri uri;

    @NonNull
    public final String mimeType;

    @NonNull
    private final UploadType type;

    protected UploadSelection(
            @NonNull Uri uri,
            @NonNull String mimeType
    ) {
        this.uri = uri;
        this.mimeType = mimeType;
        this.type = UploadType.fromMime(mimeType);
    }

    @NonNull
    public final UploadType getType() {
        return type;
    }

    public final boolean isPhoto() {
        return type == UploadType.PHOTO;
    }

    public final boolean isVideo() {
        return type == UploadType.VIDEO;
    }

    public final boolean isFile() {
        return type == UploadType.FILE;
    }

    @NonNull
    @Override
    public String toString() {
        return "UploadSelection{" +
                "uri=" + uri +
                ", mimeType='" + mimeType + '\'' +
                ", type=" + type +
                '}';
    }
}
