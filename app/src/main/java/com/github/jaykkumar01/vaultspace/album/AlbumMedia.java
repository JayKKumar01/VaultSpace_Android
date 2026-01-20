package com.github.jaykkumar01.vaultspace.album;

import com.github.jaykkumar01.vaultspace.models.base.UploadType;
import com.github.jaykkumar01.vaultspace.models.base.UploadedItem;

public class AlbumMedia {

    public final String fileId;
    public String name;
    public String mimeType;
    public long modifiedTime;
    public long sizeBytes;
    public String thumbnailLink;
    public boolean isVideo;

    public AlbumMedia(UploadedItem item) {
        this.fileId = item.fileId;
        this.name = item.name;
        this.mimeType = item.mimeType;
        this.modifiedTime = item.modifiedTime;
        this.sizeBytes = item.sizeBytes;
        this.thumbnailLink = item.thumbnailLink;
        this.isVideo = item.getType() == UploadType.VIDEO;
    }
}
