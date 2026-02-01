package com.github.jaykkumar01.vaultspace.album.layout;

import com.github.jaykkumar01.vaultspace.album.band.Band;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.album.model.AspectClass;

import java.util.ArrayList;
import java.util.List;

public final class PairingEngine {

    private static final int PAIR_THRESHOLD = 2;

    private PairingEngine() {}

    /* ============================================================
       Public API (SINGLE GROUP)
       ============================================================ */

    /**
     * Input:
     *  - media: already sorted DESC by momentMillis
     *  - timeLabel: label for this group ("Today", "Jan 2025", etc.)
     *
     * Output:
     *  - paired bands with label applied
     */
    public static List<Band> pair(List<AlbumMedia> media, String timeLabel) {
        List<Band> out = new ArrayList<>();
        if (media == null || media.isEmpty()) return out;

        int n = media.size();
        boolean[] used = new boolean[n];

        for (int i = 0; i < n; i++) {
            if (used[i]) continue;

            AlbumMedia a = media.get(i);
            int best = -1, bestScore = 0;

            for (int j = i + 1; j < n; j++) {
                if (used[j]) continue;
                int s = score(a, media.get(j));
                if (s > bestScore) {
                    bestScore = s;
                    best = j;
                }
            }

            if (bestScore >= PAIR_THRESHOLD) {
                used[i] = used[best] = true;
                out.add(new Band(a, media.get(best), timeLabel));
            } else {
                used[i] = true;
                out.add(new Band(a, null, timeLabel));
            }
        }

        return out;
    }

    /* ============================================================
       Pairing Scoring (UNCHANGED)
       ============================================================ */

    private static int score(AlbumMedia a, AlbumMedia b) {
        AspectClass A = AspectClass.of(a);
        AspectClass B = AspectClass.of(b);

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
