package com.github.jaykkumar01.vaultspace.album.band;

public final class TimeBucket {

    public final TimeBucketType type;
    public final String key;      // e.g. "2026-01" for MONTH
    public final long startMillis;
    public final long endMillis;

    public TimeBucket(
            TimeBucketType type,
            String key,
            long startMillis,
            long endMillis
    ){
        this.type=type;
        this.key=key;
        this.startMillis=startMillis;
        this.endMillis=endMillis;
    }

    public boolean contains(long t){
        return t>=startMillis && t<=endMillis;
    }
}
