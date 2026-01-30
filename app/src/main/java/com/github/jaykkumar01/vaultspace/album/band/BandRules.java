package com.github.jaykkumar01.vaultspace.album.band;

public final class BandRules {

    private BandRules(){}

    // Hard blockers
    public static final float EXTREME_WIDE = 1.6f;
    public static final float ASPECT_MISMATCH = 2.2f;

    // Similarity
    public static final float SIMILAR_AR_DELTA = 0.35f;

    // Shape classes
    public static final float VERY_TALL = 0.7f; // vertical dominant
    public static final float WIDE = 1.3f;      // horizontal dominant
}

