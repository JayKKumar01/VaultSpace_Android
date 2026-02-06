package com.github.jaykkumar01.vaultspace.core.upload.base;

import android.net.Uri;

public interface UploadObserver {

    void onSnapshot(UploadSnapshot snapshot);
    void onCancelled();

    void onSuccess(UploadedItem item, Uri uri);
    void onFailure(UploadSelection selection);

    void onProgress(UploadSelection selection, long uploadedBytes, long totalBytes);
}
