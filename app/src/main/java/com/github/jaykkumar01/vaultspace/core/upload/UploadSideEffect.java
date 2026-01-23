package com.github.jaykkumar01.vaultspace.core.upload;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.upload.base.FailureReason;
import com.github.jaykkumar01.vaultspace.core.upload.drive.UploadDriveHelper;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadedItem;

public interface UploadSideEffect {
    void onUploadSuccess(@NonNull String groupId, @NonNull UploadedItem item);
    void onUploadFailure(@NonNull String groupId, @NonNull UploadSelection selection, FailureReason reason);
}
