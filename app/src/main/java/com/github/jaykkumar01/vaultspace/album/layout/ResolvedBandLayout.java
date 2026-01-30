package com.github.jaykkumar01.vaultspace.album.layout;

import java.util.List;

public final class ResolvedBandLayout {

    public final int bandIndex;
    public final int bandHeightPx;
    public final List<ResolvedItemFrame> items;

    public ResolvedBandLayout(
            int bandIndex,
            int bandHeightPx,
            List<ResolvedItemFrame> items
    ){
        this.bandIndex=bandIndex;
        this.bandHeightPx=bandHeightPx;
        this.items=items;
    }
}
