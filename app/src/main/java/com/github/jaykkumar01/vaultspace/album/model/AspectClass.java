package com.github.jaykkumar01.vaultspace.album.model;

public enum AspectClass {
    WIDE,
    NEUTRAL,
    TALL;

    private static final float MAX_ASPECT = 16f / 9f;

    public static AspectClass of(AlbumMedia m) {
        if (m == null) return null;

        float r = m.aspectRatio;

        // clamp to effective (matches smartCrop)
        if (r > MAX_ASPECT) r = MAX_ASPECT;
        else if (r < 1f / MAX_ASPECT) r = 1f / MAX_ASPECT;

        if (r > 1.2f) return WIDE;
        if (r >= 0.83f) return NEUTRAL;
        return TALL;
    }
}
