package com.github.jaykkumar01.vaultspace.album.layout;

public final class ResolvedItemFrame {

    public final int widthPx;
    public final int heightPx;

    // Base position relative to band top-left
    public final int xPx;
    public final int yPx;

    // Organic decoration (Perlin-driven)
    public final float offsetXPx;
    public final float offsetYPx;
    public final float rotationDeg;

    // Debug-only (safe to remove later)
    public final long debugMomentMillis;

    public ResolvedItemFrame(
            int widthPx,
            int heightPx,
            int xPx,
            int yPx,
            float offsetXPx,
            float offsetYPx,
            float rotationDeg,
            long debugMomentMillis
    ){
        this.widthPx=widthPx;
        this.heightPx=heightPx;
        this.xPx=xPx;
        this.yPx=yPx;
        this.offsetXPx=offsetXPx;
        this.offsetYPx=offsetYPx;
        this.rotationDeg=rotationDeg;
        this.debugMomentMillis=debugMomentMillis;
    }
}
