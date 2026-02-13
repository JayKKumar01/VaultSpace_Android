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

    /* ---------------- Playback Critical ---------------- */

    public final float aspectRatio;
    public final int rotation;
    public final long durationMillis;

    /* ---------------- Startup Optimization ---------------- */

    /**
     * Exact number of bytes required from start of file
     * to reach PLAYER_READY.
     * <p>
     * Range: 0 → headRequiredBytes
     */
    public long headRequiredBytes;

    /**
     * Exact tail probe size.
     * <p>
     * Range: (fileSize - tailRequiredBytes) → fileSize
     */
    public long tailRequiredBytes;

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

        // default 0 (means no prefetch optimization)
        this.headRequiredBytes = 64 * 1024;
        this.tailRequiredBytes = 64 * 1024;
    }

    /* ---------------- Convenience ---------------- */

    public boolean hasStartupLayoutInfo() {
        return headRequiredBytes > 0 || tailRequiredBytes > 0;
    }

    public long getTailStartPosition() {
        if (tailRequiredBytes <= 0 || sizeBytes <= 0) return -1;
        return sizeBytes - tailRequiredBytes;
    }
}
