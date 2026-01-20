package com.github.jaykkumar01.vaultspace.core.upload;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.upload.drive.UploadDriveHelper;
import com.github.jaykkumar01.vaultspace.models.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.models.base.UploadedItem;

public interface UploadSideEffect {
    void onUploadSuccess(@NonNull String groupId, @NonNull UploadedItem item);
    void onUploadFailure(@NonNull String groupId, @NonNull UploadSelection selection, UploadDriveHelper.FailureReason reason);
}
