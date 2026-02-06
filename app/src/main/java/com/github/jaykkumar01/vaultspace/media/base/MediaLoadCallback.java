package com.github.jaykkumar01.vaultspace.media.base;

public interface MediaLoadCallback {
    void onMediaLoading();
    void onMediaReady();
    void onMediaError(Throwable t);
}
