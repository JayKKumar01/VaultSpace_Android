package com.github.jaykkumar01.vaultspace.album;

import com.github.jaykkumar01.vaultspace.core.upload.base.UploadType;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadedItem;

public class AlbumMedia {

    public final String fileId;
    public String name;
    public String mimeType;

    public long originMoment;     // 🟢 when this media was born
    public long momentMillis;     // 🟢 last meaningful change
    public boolean vsOrigin;


    public long sizeBytes;
    public String thumbnailLink;
    public boolean isVideo;

    public AlbumMedia(UploadedItem item) {
        this.fileId = item.fileId;
        this.name = item.name;
        this.mimeType = item.mimeType;
        this.originMoment = item.originMoment;
        this.momentMillis = item.momentMillis;
        this.vsOrigin = item.vsOrigin;
        this.sizeBytes = item.sizeBytes;
        this.thumbnailLink = item.thumbnailLink;
        this.isVideo = item.getType() == UploadType.VIDEO;
    }
}
