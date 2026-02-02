package com.github.jaykkumar01.vaultspace.album.view;

import com.github.jaykkumar01.vaultspace.album.band.Band;
import com.github.jaykkumar01.vaultspace.album.band.TimeBucketizer;
import com.github.jaykkumar01.vaultspace.album.layout.BandLayout;
import com.github.jaykkumar01.vaultspace.album.layout.BandLayoutEngine;
import com.github.jaykkumar01.vaultspace.album.layout.PairingEngine;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;

import java.util.List;

public final class LayoutGroupBuilder {

    /* ================= Bands ================= */

    public List<Band> buildBands(List<AlbumMedia> media, TimeBucketizer bucketizer) {
        if (media == null || media.isEmpty()) return List.of();
        AlbumMedia first = media.get(0);
        TimeBucketizer.Result r = bucketizer.resolve(first.momentMillis);
        return PairingEngine.pair(media, r.label);
    }

    /* ================= Layouts (bulk) ================= */

    public List<BandLayout> buildLayouts(String albumId, int width, List<Band> bands) {
        if (bands == null || bands.isEmpty()) return List.of();
        BandLayoutEngine engine = new BandLayoutEngine(albumId, width);
        return engine.computeAll(bands);
    }
}
