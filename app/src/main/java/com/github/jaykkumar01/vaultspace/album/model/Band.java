package com.github.jaykkumar01.vaultspace.album.model;

import java.util.List;

public final class Band {

    public final int bandIndex;
    public final List<AlbumItem> items;
    public final int adapterStartPosition;
    public final long anchorMoment;

    public Band(int bandIndex,List<AlbumItem> items){
        this.bandIndex=bandIndex;
        this.items=items;
        this.adapterStartPosition=items.get(0).adapterPosition;
        this.anchorMoment=items.get(0).media.momentMillis;
    }

    public boolean isSolo(){
        return items.size()==1;
    }
}
