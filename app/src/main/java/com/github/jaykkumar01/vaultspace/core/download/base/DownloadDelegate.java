package com.github.jaykkumar01.vaultspace.core.download.base;

public interface DownloadDelegate {
    void enqueue(DownloadRequest media);
    void cancelAll();
}
