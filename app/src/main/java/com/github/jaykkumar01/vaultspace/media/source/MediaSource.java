package com.github.jaykkumar01.vaultspace.media.source;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;

public interface MediaSource {

    void prepare(@NonNull AlbumMedia media,
                 @NonNull MediaSourceCallback callback);

    void release();
}
