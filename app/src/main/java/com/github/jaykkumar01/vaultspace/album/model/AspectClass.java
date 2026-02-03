package com.github.jaykkumar01.vaultspace.album.model;

public enum AspectClass {

    PORTRAIT_STRONG,   // 9:16   (~0.56)
    PORTRAIT_SOFT,     // 3:4    (~0.75)
    PORTRAIT_SOCIAL,  // 4:5    (~0.80)
    SQUARE,            // 1:1    (1.00)
    LANDSCAPE_CLASSIC,// 4:3    (~1.33)
    LANDSCAPE_DSLR,   // 3:2    (~1.50)
    LANDSCAPE_WIDE;   // 16:9   (~1.78)

    private static final float MAX_ASPECT = 16f / 9f;

    public static AspectClass of(AlbumMedia m) {
        if (m == null) return null;

        float r = m.aspectRatio;

        // clamp to effective (matches smartCrop)
        if (r > MAX_ASPECT) r = MAX_ASPECT;
        else if (r < 1f / MAX_ASPECT) r = 1f / MAX_ASPECT;

        if (r < 0.65f) return PORTRAIT_STRONG;
        if (r < 0.78f) return PORTRAIT_SOFT;
        if (r < 0.90f) return PORTRAIT_SOCIAL;
        if (r < 1.15f) return SQUARE;
        if (r < 1.42f) return LANDSCAPE_CLASSIC;
        if (r < 1.65f) return LANDSCAPE_DSLR;
        return LANDSCAPE_WIDE;
    }
}
