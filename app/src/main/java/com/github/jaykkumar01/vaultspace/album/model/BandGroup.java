package com.github.jaykkumar01.vaultspace.album.model;

import com.github.jaykkumar01.vaultspace.album.band.Band;

import java.util.List;

public final class BandGroup {
    public final String key;     // TODAY / YESTERDAY / 2026-01
    public final List<Band> bands;

    public BandGroup(String key, List<Band> bands) {
        this.key = key;
        this.bands = bands;
    }
}
