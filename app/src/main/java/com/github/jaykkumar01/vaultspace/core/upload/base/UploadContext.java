package com.github.jaykkumar01.vaultspace.core.upload.base;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class UploadContext {

    @NonNull public final String uploadId;   // UploadSelection.id
    @NonNull public final String groupId;
    @Nullable public FailureReason failureReason;


    public UploadContext(@NonNull String uploadId, @NonNull String groupId) {
        this.uploadId = uploadId;
        this.groupId = groupId;
    }
}
