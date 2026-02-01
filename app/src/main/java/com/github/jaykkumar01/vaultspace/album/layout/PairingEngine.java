package com.github.jaykkumar01.vaultspace.album.layout;

import com.github.jaykkumar01.vaultspace.album.band.Band;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.album.model.AspectClass;

import java.util.ArrayList;
import java.util.List;

public final class PairingEngine {

    private static final int PAIR_THRESHOLD = 0;

    private PairingEngine() {}

    /* ============================================================
       Public API (SINGLE GROUP, O(N), ORDER-STABLE)
       ============================================================ */

    /**
     * Sequential pairing.
     *
     * Rules:
     * - media is already sorted DESC by momentMillis
     * - only consecutive items may be paired
     * - order is strictly preserved
     * - no cross-skip is possible
     */
    public static List<Band> pair(List<AlbumMedia> media, String timeLabel) {
        List<Band> out = new ArrayList<>();
        if (media == null || media.isEmpty()) return out;

        final int n = media.size();
        int i = 0;

        while (i < n) {
            AlbumMedia first = media.get(i);

            // Null safety
            if (first == null) {
                i++;
                continue;
            }

            // Last item → must be solo
            if (i == n - 1) {
                out.add(new Band(first, null, timeLabel));
                break;
            }

            AlbumMedia second = media.get(i + 1);

            // If next is invalid → solo
            if (second == null) {
                out.add(new Band(first, null, timeLabel));
                i++;
                continue;
            }

            int score = score(first, second);

            if (score >= PAIR_THRESHOLD) {
                // Pair consecutive items
                out.add(new Band(first, second, timeLabel));
                i += 2; // consume both
            } else {
                // No valid pair
                out.add(new Band(first, null, timeLabel));
                i++;
            }
        }

        return out;
    }

    /* ============================================================
       Pairing Scoring (UNCHANGED, SOURCE OF TRUTH)
       ============================================================ */

    private static int score(AlbumMedia a, AlbumMedia b) {
        if (a == null || b == null) return -1;

        AspectClass A = AspectClass.of(a);
        AspectClass B = AspectClass.of(b);

        if (A == null || B == null) return -1;

        if ((A == AspectClass.WIDE && B == AspectClass.VERY_TALL) ||
                (B == AspectClass.WIDE && A == AspectClass.VERY_TALL)) {
            return -1;
        }

        if (A == B) return 3;
        if (A == AspectClass.NEUTRAL || B == AspectClass.NEUTRAL) return 2;
        if ((A == AspectClass.WIDE && B == AspectClass.TALL) ||
                (A == AspectClass.TALL && B == AspectClass.WIDE)) return 1;

        return 0;
    }
}
