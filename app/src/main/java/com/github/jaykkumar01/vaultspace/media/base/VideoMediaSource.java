package com.github.jaykkumar01.vaultspace.media.base;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;

public interface VideoMediaSource {
    void prepare(@NonNull AlbumMedia media,
                 @NonNull VideoMediaPrepareCallback callback);
    void release();       // 🔹 full teardown
}

