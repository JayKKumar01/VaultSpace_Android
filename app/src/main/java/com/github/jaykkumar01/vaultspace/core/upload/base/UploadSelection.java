package com.github.jaykkumar01.vaultspace.core.upload.base;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class UploadSelection {

    @NonNull  public final Uri uri;
    @Nullable public final String mimeType;
    @NonNull  public final UploadType type;
    @NonNull  public final String displayName;
    public final long sizeBytes;
    public final long momentMillis;
    @Nullable public final String thumbnailPath;

    // Optional & mutable – populated only on failure
    @Nullable public FailureReason failureReason;

    public UploadSelection(
            @NonNull Uri uri,
            @Nullable String mimeType,
            @NonNull String displayName,
            long sizeBytes,
            long momentMillis,
            @Nullable String thumbnailPath
    ) {
        this.uri = uri;
        this.mimeType = mimeType;
        this.type = mimeType != null ? UploadType.fromMime(mimeType) : UploadType.FILE;
        this.displayName = displayName;
        this.sizeBytes = sizeBytes;
        this.momentMillis = momentMillis;
        this.thumbnailPath = thumbnailPath;
        this.failureReason = null;
    }

    public void markFailed(@NonNull FailureReason reason) {
        this.failureReason = reason;
    }

}
