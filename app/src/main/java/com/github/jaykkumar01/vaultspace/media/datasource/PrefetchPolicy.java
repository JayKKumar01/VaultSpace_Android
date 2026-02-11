package com.github.jaykkumar01.vaultspace.media.datasource;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;

public final class PrefetchPolicy {

    private static final int BUFFER_CAP=8*1024*1024;
    private static final int PREFETCH_SECONDS=5;
    private static final int MIN_PREFETCH=256*1024;

    private final long avgBitrateBytesPerSec;
    private final int basePrefetchLimitBytes;

    public PrefetchPolicy(AlbumMedia media){
        this.avgBitrateBytesPerSec=computeBitrate(media);
        this.basePrefetchLimitBytes=computeLimit();
    }

    private long computeBitrate(AlbumMedia m){
        if(!m.isVideo||m.durationMillis<=0||m.sizeBytes<=0) return -1;
        long sec=m.durationMillis/1000L;
        if(sec<=0) return -1;
        return m.sizeBytes/sec;
    }

    private int computeLimit(){
        if(avgBitrateBytesPerSec<=0) return BUFFER_CAP;
        long target=avgBitrateBytesPerSec*PREFETCH_SECONDS;
        if(target<MIN_PREFETCH) target=MIN_PREFETCH;
        if(target>BUFFER_CAP) target=BUFFER_CAP;
        return (int)target;
    }

    public long getAverageBitrateBytesPerSec(){ return avgBitrateBytesPerSec; }

    public int getBasePrefetchLimitBytes(){ return basePrefetchLimitBytes; }
}
