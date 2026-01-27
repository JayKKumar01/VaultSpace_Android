package com.github.jaykkumar01.vaultspace.core.upload.base;

public interface UploadObserver {

    void onSnapshot(UploadSnapshot snapshot);
    void onCancelled();

    void onSuccess(UploadedItem item);
    void onFailure(UploadSelection selection);

    void onProgress(String uId, String name, String thumbnailPath, long uploadedBytes, long totalBytes);
}
