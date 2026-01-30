package com.github.jaykkumar01.vaultspace.album.layout;

public final class BandLayout {

    public final int bandIndex;
    public final float height;
    public final float offsetX;
    public final float offsetY;
    public final float rotation;

    public BandLayout(
            int bandIndex,
            float height,
            float offsetX,
            float offsetY,
            float rotation
    ){
        this.bandIndex=bandIndex;
        this.height=height;
        this.offsetX=offsetX;
        this.offsetY=offsetY;
        this.rotation=rotation;
    }
}
