package com.github.jaykkumar01.vaultspace.album;

public final class MediaGeometry {
    public final float aspectRatio; // width / height (already rotation-corrected)
    public final int rotation;       // 0, 90, 180, 270

    public MediaGeometry(float aspectRatio, int rotation) {
        this.aspectRatio = aspectRatio;
        this.rotation = rotation;
    }
}
