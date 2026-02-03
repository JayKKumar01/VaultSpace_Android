package com.github.jaykkumar01.vaultspace.album.view;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;

public interface OnMediaActionListener {
    void onMediaClick(@NonNull AlbumMedia media);
    void onMediaLongPress(@NonNull AlbumMedia media);
}
