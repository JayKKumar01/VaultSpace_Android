package com.github.jaykkumar01.vaultspace.album.model;

public enum AspectClass {
    WIDE,
    NEUTRAL,
    TALL,
    VERY_TALL;

    public static AspectClass of(AlbumMedia m){
        float r=m.aspectRatio;
        if(r>1.3f) return WIDE;
        if(r>=0.8f) return NEUTRAL;
        if(r>=0.6f) return TALL;
        return VERY_TALL;
    }
}
