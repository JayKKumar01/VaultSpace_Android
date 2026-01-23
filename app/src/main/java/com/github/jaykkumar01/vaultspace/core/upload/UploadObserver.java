package com.github.jaykkumar01.vaultspace.core.upload;

import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadedItem;

public interface UploadObserver {

    void onSnapshot(UploadSnapshot snapshot);
    void onCancelled();

    void onSuccess(UploadedItem item);
    void onFailure(UploadSelection selection);

    void onProgress(String name, long uploadedBytes, long totalBytes);
}
