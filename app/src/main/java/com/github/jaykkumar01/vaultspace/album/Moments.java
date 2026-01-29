package com.github.jaykkumar01.vaultspace.album;

public final class Moments {
    public final long originMoment;
    public final long momentMillis;
    public final boolean vsOrigin;

    public Moments(long originMoment, long momentMillis, boolean vsOrigin) {
        this.originMoment = originMoment;
        this.momentMillis = momentMillis;
        this.vsOrigin = vsOrigin;
    }
}
