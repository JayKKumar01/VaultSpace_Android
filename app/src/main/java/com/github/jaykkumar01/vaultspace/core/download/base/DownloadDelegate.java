package com.github.jaykkumar01.vaultspace.core.download.base;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;

public interface DownloadDelegate {
    void enqueue(AlbumMedia media);
    void cancelAll();
}
