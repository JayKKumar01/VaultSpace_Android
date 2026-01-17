package com.github.jaykkumar01.vaultspace.core.session.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "upload_failure",
        indices = {
                @Index(value = { "groupId", "uri", "type" }, unique = true)
        }
)
public final class UploadFailureEntity {

    @PrimaryKey(autoGenerate = true)
    public final long id;

    @NonNull
    public final String groupId;

    @NonNull
    public final String uri; // uri.toString() → same as RetryStore

    @NonNull
    public final String displayName;

    @NonNull
    public final String type; // UploadType.name()

    public final String thumbnailPath; // nullable

    @NonNull
    public final String failureReason; // enum name

    public UploadFailureEntity(
            long id,
            @NonNull String groupId,
            @NonNull String uri,
            @NonNull String displayName,
            @NonNull String type,
            String thumbnailPath,
            @NonNull String failureReason
    ) {
        this.id = id;
        this.groupId = groupId;
        this.uri = uri;
        this.displayName = displayName;
        this.type = type;
        this.thumbnailPath = thumbnailPath;
        this.failureReason = failureReason;
    }
}
