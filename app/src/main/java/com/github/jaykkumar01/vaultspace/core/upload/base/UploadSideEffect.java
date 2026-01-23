package com.github.jaykkumar01.vaultspace.core.upload.base;

import androidx.annotation.NonNull;

public interface UploadSideEffect {
    void onUploadSuccess(@NonNull String groupId, @NonNull UploadedItem item);
    void onUploadFailure(@NonNull String groupId, @NonNull UploadSelection selection, FailureReason reason);
}
