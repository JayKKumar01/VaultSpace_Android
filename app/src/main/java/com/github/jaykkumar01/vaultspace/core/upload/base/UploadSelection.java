package com.github.jaykkumar01.vaultspace.core.upload.base;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class UploadSelection {

    @NonNull
    public final String id;
    @NonNull
    public final Uri uri;
    @Nullable
    public final String mimeType;
    @NonNull
    public final UploadType type;
    @NonNull
    public final String displayName;
    public final long sizeBytes;

    public final long originMoment;
    public final long momentMillis;

    public final float aspectRatio;
    public final int rotation;

    public final long durationMillis; // 🔑 VIDEO ONLY (0 for non-video)

    @Nullable
    public final String thumbnailPath;
    @NonNull
    public final UploadContext context;

    public UploadSelection(
            @NonNull String id,
            @NonNull String groupId,
            @NonNull Uri uri,
            @Nullable String mimeType,
            @NonNull String displayName,
            long sizeBytes,
            long originMoment,
            long momentMillis,
            float aspectRatio,
            int rotation,
            long durationMillis,
            @Nullable String thumbnailPath
    ) {
        this.id = id;
        this.uri = uri;
        this.mimeType = mimeType;
        this.type = mimeType != null ? UploadType.fromMime(mimeType) : UploadType.FILE;
        this.displayName = displayName;
        this.sizeBytes = sizeBytes;
        this.originMoment = originMoment;
        this.momentMillis = momentMillis;
        this.aspectRatio = aspectRatio;
        this.rotation = rotation;
        this.durationMillis = durationMillis;
        this.thumbnailPath = thumbnailPath;
        this.context = new UploadContext(id, groupId);
    }
}
