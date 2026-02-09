package com.github.jaykkumar01.vaultspace.core.session.db.retry;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;

@Entity(
        tableName = "upload_retry",
        primaryKeys = { "id" }
)
public final class UploadRetryEntity {

    @NonNull public final String id;          // UploadSelection.id
    @NonNull public final String groupId;

    @NonNull public final String uri;
    @Nullable public final String mimeType;

    @NonNull public final String displayName;
    public final long sizeBytes;

    public final long originMoment;
    public final long momentMillis;

    // 🔑 layout-critical (NEW)
    public final float aspectRatio;
    public final int rotation;
    public final long durationMillis; // 🔑 VIDEO ONLY (0 for non-video)


    @Nullable public final String thumbnailPath;
    @NonNull public final String failureReason;

    public UploadRetryEntity(
            @NonNull String id,
            @NonNull String groupId,
            @NonNull String uri,
            @Nullable String mimeType,
            @NonNull String displayName,
            long sizeBytes,
            long originMoment,
            long momentMillis,
            float aspectRatio,
            int rotation,
            long durationMillis,
            @Nullable String thumbnailPath,
            @NonNull String failureReason
    ) {
        this.id = id;
        this.groupId = groupId;
        this.uri = uri;
        this.mimeType = mimeType;
        this.displayName = displayName;
        this.sizeBytes = sizeBytes;
        this.originMoment = originMoment;
        this.momentMillis = momentMillis;
        this.aspectRatio = aspectRatio;
        this.rotation = rotation;
        this.durationMillis = durationMillis;
        this.thumbnailPath = thumbnailPath;
        this.failureReason = failureReason;
    }

}
