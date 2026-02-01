package com.github.jaykkumar01.vaultspace.album.model;

public final class MediaFrame {

    public final AlbumMedia media;
    public final int width;
    public final int height;
    public int baseX;

    public MediaFrame(AlbumMedia media,int width, int height, int baseX) {
        this.media = media;
        this.width = width;
        this.height = height;
        this.baseX = baseX;
    }
}
