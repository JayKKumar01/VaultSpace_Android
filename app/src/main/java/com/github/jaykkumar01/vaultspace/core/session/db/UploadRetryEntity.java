package com.github.jaykkumar01.vaultspace.core.session.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;

@Entity(
        tableName = "upload_retry",
        primaryKeys = { "groupId","uri","type" }
)
public final class UploadRetryEntity {

    @NonNull public final String groupId;
    @NonNull public final String uri;
    @Nullable public final String mimeType;
    @NonNull public final String type;

    @NonNull public final String displayName;
    @Nullable public final String thumbnailPath;

    @NonNull public final String failureReason;

    public UploadRetryEntity(
            @NonNull String groupId,
            @NonNull String uri,
            @Nullable String mimeType,
            @NonNull String type,
            @NonNull String displayName,
            @Nullable String thumbnailPath,
            @NonNull String failureReason
    ){
        this.groupId=groupId;
        this.uri=uri;
        this.mimeType=mimeType;
        this.type=type;
        this.displayName=displayName;
        this.thumbnailPath=thumbnailPath;
        this.failureReason=failureReason;
    }
}
