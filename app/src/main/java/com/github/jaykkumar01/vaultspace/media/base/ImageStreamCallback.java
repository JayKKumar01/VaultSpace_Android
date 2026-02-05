package com.github.jaykkumar01.vaultspace.media.base;

import androidx.annotation.NonNull;

import java.io.InputStream;

public interface ImageStreamCallback {
    void onReady(@NonNull InputStream stream, long sizeBytes);

    void onError(@NonNull Exception e);
}