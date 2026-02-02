package com.github.jaykkumar01.vaultspace.album.view;

import com.github.jaykkumar01.vaultspace.album.band.Band;
import com.github.jaykkumar01.vaultspace.album.layout.BandLayout;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;

import java.util.ArrayList;
import java.util.List;

public final class Group {

    public final String key;

    /* ===== Source data ===== */
    public final List<AlbumMedia> media = new ArrayList<>();

    /* ===== Pairing layer (diff unit) ===== */
    public List<Band> bands = List.of();

    /* ===== Rendered output ===== */
    public List<BandLayout> layouts = List.of();

    public int layoutStart;
    public int layoutCount;

    Group(String key) {
        this.key = key;
    }
}
