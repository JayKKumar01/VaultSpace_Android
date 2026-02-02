package com.github.jaykkumar01.vaultspace.album.view;

import com.github.jaykkumar01.vaultspace.album.band.Band;
import com.github.jaykkumar01.vaultspace.album.band.TimeBucketizer;
import com.github.jaykkumar01.vaultspace.album.layout.BandLayout;
import com.github.jaykkumar01.vaultspace.album.layout.BandLayoutEngine;
import com.github.jaykkumar01.vaultspace.album.layout.PairingEngine;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;

import java.util.List;

public final class LayoutGroupBuilder {

    public List<BandLayout> build(String albumId,int width,List<AlbumMedia> media,TimeBucketizer b) {
        if (media == null || media.isEmpty()) return List.of();

        AlbumMedia first = media.get(0);
        TimeBucketizer.Result r = b.resolve(first.momentMillis);

        List<Band> bands = PairingEngine.pair(media, r.label);
        List<BandLayout> layouts = BandLayoutEngine.compute(albumId, width, bands);

        for (int i = 0; i < layouts.size(); i++) layouts.get(i).showTimeLabel = (i == 0);
        return layouts;
    }
}
