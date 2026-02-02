package com.github.jaykkumar01.vaultspace.album.view;

import com.github.jaykkumar01.vaultspace.album.layout.BandLayout;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;

import java.util.ArrayList;
import java.util.List;

public final class Group {
        final String key;
        final List<AlbumMedia> media = new ArrayList<>();
        List<BandLayout> layouts = List.of();
        int layoutStart;
        int layoutCount;

        Group(String key) {
            this.key = key;
        }
    }