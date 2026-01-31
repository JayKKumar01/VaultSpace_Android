package com.github.jaykkumar01.vaultspace.album.layout;

import com.github.jaykkumar01.vaultspace.album.band.Band;
import com.github.jaykkumar01.vaultspace.album.band.TimeBucket;
import com.github.jaykkumar01.vaultspace.album.band.TimeBucketizer;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.album.model.AspectClass;
import com.github.jaykkumar01.vaultspace.album.model.BandGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PairingEngine {

    private static final int PAIR_THRESHOLD = 2;
    private static final SimpleDateFormat MONTH_LABEL = new SimpleDateFormat("MMM yyyy", Locale.US);

    private PairingEngine() {
    }

    /* ============================================================
       Public Entry (GROUPED)
       ============================================================ */

    public static List<BandGroup> build(List<AlbumMedia> media) {
        List<BandGroup> groups = new ArrayList<>();
        if (media == null || media.isEmpty()) return groups;

        long now = System.currentTimeMillis();

        // -------- Stage 1: Time Bucketing --------
        BucketResult buckets = bucketize(media, now);

        // -------- Stage 2: Fixed buckets (TODAY → THIS_MONTH) --------
        for (TimeBucket bucket : buckets.orderedBuckets) {
            List<AlbumMedia> bucketMedia = buckets.fixedMedia.get(bucket.key);
            if (bucketMedia != null && !bucketMedia.isEmpty()) {
                String label = labelForBucket(bucket);
                groups.add(new BandGroup(
                        bucket.key,
                        label,
                        pairWithin(bucketMedia, label)
                ));
            }
        }

        // -------- Stage 3: Month buckets (yyyy-MM) --------
        List<String> sortedMonths = new ArrayList<>(buckets.monthBuckets.keySet());
        sortedMonths.sort((a, b) -> b.compareTo(a)); // newest first

        for (String monthKey : sortedMonths) {
            List<AlbumMedia> monthMedia = buckets.monthBuckets.get(monthKey);
            if (monthMedia != null && !monthMedia.isEmpty()) {
                String label = MONTH_LABEL.format(monthMedia.get(0).momentMillis);
                groups.add(new BandGroup(
                        monthKey,
                        label,
                        pairWithin(monthMedia, label)
                ));
            }
        }

        return groups;
    }

    /* ============================================================
       Stage 1: Time Bucketing
       ============================================================ */

    private static BucketResult bucketize(List<AlbumMedia> media, long now) {

        List<TimeBucket> fixedBuckets = TimeBucketizer.buildBuckets(now);
        Map<String, List<AlbumMedia>> fixedMedia = new HashMap<>();
        Map<String, List<AlbumMedia>> monthBuckets = new HashMap<>();

        // pre-create fixed buckets
        for (TimeBucket b : fixedBuckets) {
            fixedMedia.put(b.key, new ArrayList<>());
        }

        for (AlbumMedia m : media) {

            String key = TimeBucketizer.resolveKey(m.momentMillis, now);

            if (fixedMedia.containsKey(key)) {
                fixedMedia.computeIfAbsent(key, k -> new ArrayList<>()).add(m);
            } else {
                monthBuckets.computeIfAbsent(key, k -> new ArrayList<>()).add(m);
            }

        }

        return new BucketResult(fixedBuckets, fixedMedia, monthBuckets);
    }

    /* ============================================================
       Stage 2: Pairing Logic (UNCHANGED)
       ============================================================ */

    private static List<Band> pairWithin(List<AlbumMedia> media, String label) {
        List<Band> out = new ArrayList<>();
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
                out.add(new Band(a, media.get(best), label));
            } else {
                used[i] = true;
                out.add(new Band(a, null, label));
            }
        }

        return out;
    }

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

    /* ============================================================
       Label Helpers
       ============================================================ */

    private static String labelForBucket(TimeBucket b) {
        return switch (b.type) {
            case TODAY -> "Today";
            case YESTERDAY -> "Yesterday";
            case THIS_WEEK -> "This Week";
            case THIS_MONTH -> "This Month";
            default -> "";
        };
    }

    /* ============================================================
       Internal Holder
       ============================================================ */

    private record BucketResult(
            List<TimeBucket> orderedBuckets,
            Map<String, List<AlbumMedia>> fixedMedia,
            Map<String, List<AlbumMedia>> monthBuckets
    ) {
    }
}
