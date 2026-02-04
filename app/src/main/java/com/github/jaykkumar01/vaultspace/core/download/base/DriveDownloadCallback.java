package com.github.jaykkumar01.vaultspace.core.download.base;

public interface DriveDownloadCallback {
    void onProgress(long downloadedBytes, long totalBytes);
    void onCompleted();
    void onFailed(Exception e);
}
