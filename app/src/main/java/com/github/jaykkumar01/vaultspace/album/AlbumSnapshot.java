package com.github.jaykkumar01.vaultspace.album;

public class AlbumSnapshot {

    public final String albumId;

    public String albumName;
    public int photoCount;
    public int videoCount;
    public boolean isError;

    public AlbumSnapshot(String albumId) {
        this.albumId = albumId;
    }

    public AlbumSnapshot(
            String albumId,
            String albumName,
            int photoCount,
            int videoCount,
            boolean isError
    ) {
        this.albumId = albumId;
        this.albumName = albumName;
        this.photoCount = photoCount;
        this.videoCount = videoCount;
        this.isError = isError;
    }

    public int getTotalCount() {
        return photoCount + videoCount;
    }
}
