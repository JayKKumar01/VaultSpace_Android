package com.github.jaykkumar01.vaultspace.media.base;

public interface MediaLoadCallback {
    void onMediaLoading(String text);
    void onMediaReady();
    void onMediaError(Throwable t);
}
