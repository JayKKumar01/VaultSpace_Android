package com.github.jaykkumar01.vaultspace.album.layout;

import com.github.jaykkumar01.vaultspace.album.band.Band;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.album.model.AspectClass;

import java.util.ArrayList;
import java.util.List;

public final class PairingEngine {

    /* ================= Config ================= */

    // ONLY knob controlling pairing strictness
    private static final int PAIR_THRESHOLD = 4;

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

            // last item → solo
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
       Scoring (FULLY EXPRESSIVE, NO LIMITS)
       ============================================================ */

    private static int score(AlbumMedia a, AlbumMedia b) {
        AspectClass A = AspectClass.of(a);
        AspectClass B = AspectClass.of(b);
        if (A == null || B == null) return Integer.MIN_VALUE;

        int distance = Math.abs(A.ordinal() - B.ordinal());

        // higher = more similar
        return 6 - distance;
    }
}
