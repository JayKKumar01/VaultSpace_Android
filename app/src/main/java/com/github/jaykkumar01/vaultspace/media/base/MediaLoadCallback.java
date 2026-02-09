package com.github.jaykkumar01.vaultspace.media.base;

public interface MediaLoadCallback {
    void onMediaLoading(String message);
    void onMediaReady();
    void onMediaError(Exception e);
}
