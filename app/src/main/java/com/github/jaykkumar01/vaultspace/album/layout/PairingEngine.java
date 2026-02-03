package com.github.jaykkumar01.vaultspace.album.layout;

import com.github.jaykkumar01.vaultspace.album.band.Band;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.album.model.AspectClass;

import java.util.ArrayList;
import java.util.List;

public final class PairingEngine {

    private static final int PAIR_THRESHOLD = 1;

    private PairingEngine() {}

    /* ============================================================
       Public API (SEQUENTIAL, ORDER-STABLE)
       ============================================================ */

    public static List<Band> pair(List<AlbumMedia> media, String timeLabel) {
        List<Band> out = new ArrayList<>();
        if (media == null || media.isEmpty()) return out;

        final int n = media.size();
        int i = 0;

        while (i < n) {
            AlbumMedia first = media.get(i);
            if (first == null) { i++; continue; }

            if (i == n - 1) {
                out.add(new Band(first, null, timeLabel));
                break;
            }

            AlbumMedia second = media.get(i + 1);
            if (second == null) {
                out.add(new Band(first, null, timeLabel));
                i++;
                continue;
            }

            if (score(first, second) >= PAIR_THRESHOLD) {
                out.add(new Band(first, second, timeLabel));
                i += 2;
            } else {
                out.add(new Band(first, null, timeLabel));
                i++;
            }
        }

        return out;
    }

    /* ============================================================
       Scoring (BASED ON EFFECTIVE ASPECT)
       ============================================================ */

    private static int score(AlbumMedia a, AlbumMedia b) {
        AspectClass A = AspectClass.of(a);
        AspectClass B = AspectClass.of(b);
        if (A == null || B == null) return -1;

        if (A == B) return 3;
        if (A == AspectClass.NEUTRAL || B == AspectClass.NEUTRAL) return 2;
        return 1;
    }
}
