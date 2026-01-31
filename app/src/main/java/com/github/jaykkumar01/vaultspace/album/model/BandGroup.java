package com.github.jaykkumar01.vaultspace.album.model;

import com.github.jaykkumar01.vaultspace.album.band.Band;

import java.util.List;

public final class BandGroup {
    public final String key;     // TODAY / YESTERDAY / 2026-01
    public final String label;          // "Today", "Jan 2026"
    public final List<Band> bands;

    public BandGroup(String key, String label, List<Band> bands) {
        this.key = key;
        this.label = label;
        this.bands = bands;
    }
}
