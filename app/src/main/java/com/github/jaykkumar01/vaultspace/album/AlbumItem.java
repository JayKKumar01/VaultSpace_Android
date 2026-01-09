package com.github.jaykkumar01.vaultspace.album;

public class AlbumItem {

    public final String fileId;

    public String name;
    public String mimeType;
    public long modifiedTime;
    public long sizeBytes;
    public String thumbnailLink;
    public boolean isVideo;

    public AlbumItem(String fileId) {
        this.fileId = fileId;
    }

    public AlbumItem(
            String fileId,
            String name,
            String mimeType,
            long modifiedTime,
            long sizeBytes,
            String thumbnailLink
    ) {
        this.fileId = fileId;
        this.name = name;
        this.mimeType = mimeType;
        this.modifiedTime = modifiedTime;
        this.sizeBytes = sizeBytes;
        this.thumbnailLink = thumbnailLink;
        this.isVideo = mimeType != null && mimeType.startsWith("video/");
    }
}
