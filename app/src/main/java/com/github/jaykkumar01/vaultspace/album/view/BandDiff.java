package com.github.jaykkumar01.vaultspace.album.view;

import com.github.jaykkumar01.vaultspace.album.band.Band;
import java.util.List;

public final class BandDiff {

    public final int start;          // band index
    public final int removeCount;    // bands to remove
    public final List<Band> items;   // bands to insert

    public BandDiff(int start, int removeCount, List<Band> items) {
        this.start = start;
        this.removeCount = removeCount;
        this.items = items;
    }
}
