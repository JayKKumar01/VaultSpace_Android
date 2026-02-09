package com.github.jaykkumar01.vaultspace.album.model;

import com.github.jaykkumar01.vaultspace.core.upload.base.UploadType;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadedItem;

public final class AlbumMedia {

    public final String fileId;
    public String name;
    public String mimeType;

    public long originMoment;
    public long momentMillis;
    public boolean vsOrigin;

    public long sizeBytes;
    public String thumbnailLink;
    public boolean isVideo;

    // 🔑 layout / playback-critical
    public final float aspectRatio;
    public final int rotation;
    public final long durationMillis;   // 🟢 VIDEO ONLY

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

        this.aspectRatio = item.aspectRatio;
        this.rotation = item.rotation;
        this.durationMillis = item.durationMillis;
    }
}
