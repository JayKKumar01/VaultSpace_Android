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
    public final long momentMillis;

    @Nullable public final String thumbnailPath;


    @NonNull public final String failureReason;

    public UploadRetryEntity(
            @NonNull String id,
            @NonNull String groupId,
            @NonNull String uri,
            @Nullable String mimeType,
            @NonNull String displayName,
            long sizeBytes,
            long momentMillis,
            @Nullable String thumbnailPath,
            @NonNull String failureReason
    ) {
        this.id = id;
        this.groupId = groupId;
        this.uri = uri;
        this.mimeType = mimeType;
        this.displayName = displayName;
        this.sizeBytes = sizeBytes;
        this.momentMillis = momentMillis;
        this.thumbnailPath = thumbnailPath;
        this.failureReason = failureReason;
    }
}
