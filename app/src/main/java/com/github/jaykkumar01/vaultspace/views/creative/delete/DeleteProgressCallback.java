package com.github.jaykkumar01.vaultspace.views.creative.delete;

public interface DeleteProgressCallback {
    void onStart(int totalFiles);
    void onFileDeleting(String fileName, int deleted, int total);
    void onCompleted();
    void onError(Exception e);
}
