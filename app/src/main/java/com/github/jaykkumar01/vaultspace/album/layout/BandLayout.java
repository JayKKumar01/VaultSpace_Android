package com.github.jaykkumar01.vaultspace.album.layout;

import com.github.jaykkumar01.vaultspace.album.model.MediaFrame;

public final class BandLayout {

    public final String timeLabel;     // ⬅️ NEW
    public final int bandHeight;
    public final MediaFrame[] frames;  // size 1 or 2

    public BandLayout(String timeLabel,int bandHeight,MediaFrame[] frames){
        this.timeLabel=timeLabel;
        this.bandHeight=bandHeight;
        this.frames=frames;
    }
}
