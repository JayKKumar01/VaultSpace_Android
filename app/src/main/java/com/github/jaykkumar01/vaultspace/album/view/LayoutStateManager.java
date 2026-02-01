package com.github.jaykkumar01.vaultspace.album.view;

import com.github.jaykkumar01.vaultspace.album.band.Band;
import com.github.jaykkumar01.vaultspace.album.band.TimeBucketizer;
import com.github.jaykkumar01.vaultspace.album.layout.BandLayout;
import com.github.jaykkumar01.vaultspace.album.layout.BandLayoutEngine;
import com.github.jaykkumar01.vaultspace.album.layout.PairingEngine;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LayoutStateManager {

    /* ================= Internal State ================= */

    private final List<AlbumMedia> media = new ArrayList<>();
    private final List<BandLayout> flatLayouts = new ArrayList<>();

    private final List<Group> groups = new ArrayList<>();
    private final Map<String, Group> groupByKey = new HashMap<>();

    private TimeBucketizer bucketizer;

    /* ================= Public API ================= */

    public LayoutResult setMedia(String albumId, int width, List<AlbumMedia> sortedMedia) {
        clear();

        if (sortedMedia == null || sortedMedia.isEmpty())
            return LayoutResult.full(flatLayouts);

        media.addAll(sortedMedia);
        bucketizer = TimeBucketizer.create(System.currentTimeMillis());

        // group media
        for (AlbumMedia m : media) {
            String key = bucketKey(m);
            groupByKey
                    .computeIfAbsent(key, k -> {
                        Group g = new Group(k);
                        groups.add(g);
                        return g;
                    })
                    .media.add(m);
        }

        // order groups
        groups.sort(Comparator.comparingInt(g -> groupOrder(g.key)));

        // build layouts
        int layoutCursor = 0;
        for (Group g : groups) {
            g.layoutStart = layoutCursor;
            g.layouts = buildGroupLayouts(albumId, width, g);
            g.layoutCount = g.layouts.size();
            flatLayouts.addAll(g.layouts);
            layoutCursor += g.layoutCount;
        }

        return LayoutResult.full(flatLayouts);
    }

    public LayoutResult addMedia(String albumId, int width, AlbumMedia m) {
        if (m == null) return LayoutResult.full(flatLayouts);

        insertSorted(media, m);

        String key = bucketKey(m);
        Group g = groupByKey.get(key);

        // new group
        if (g == null) {
            g = new Group(key);
            g.media.add(m);
            insertGroupOrdered(g);
            rebuildGroup(albumId, width, g, true);
            return LayoutResult.range(g.layoutStart, 0, g.layouts);
        }

        // existing group
        insertSorted(g.media, m);
        int oldCount = g.layoutCount;
        rebuildGroup(albumId, width, g, false);

        return LayoutResult.range(g.layoutStart, oldCount, g.layouts);
    }

    public LayoutResult removeMedia(String albumId, int width, AlbumMedia m) {
        if (m == null) return LayoutResult.full(flatLayouts);

        String key = bucketKey(m);
        Group g = groupByKey.get(key);
        if (g == null || !g.media.remove(m)) return LayoutResult.full(flatLayouts);

        media.remove(m);

        int oldCount = g.layoutCount;

        // group becomes empty
        if (g.media.isEmpty()) {
            removeGroup(g);
            return LayoutResult.range(g.layoutStart, oldCount, List.of());
        }

        rebuildGroup(albumId, width, g, false);
        return LayoutResult.range(g.layoutStart, oldCount, g.layouts);
    }

    /* ================= Group Rebuild ================= */

    private void rebuildGroup(String albumId, int width, Group g, boolean isNew) {
        List<BandLayout> old = g.layouts;

        g.layouts = buildGroupLayouts(albumId, width, g);
        g.layoutCount = g.layouts.size();

        if (!isNew) {
            for (int i = 0; i < old.size(); i++)
                flatLayouts.remove(g.layoutStart);
        }

        flatLayouts.addAll(g.layoutStart, g.layouts);
        shiftFollowingGroups(g, g.layoutCount - old.size());
    }

    private List<BandLayout> buildGroupLayouts(String albumId, int width, Group g) {
        AlbumMedia first = g.media.get(0);

        TimeBucketizer.Bucket bucket = bucketizer.bucketOf(first.momentMillis);
        String label = bucketizer.labelOf(bucket, first.momentMillis);

        List<Band> bands = PairingEngine.pair(g.media, label);
        List<BandLayout> layouts = BandLayoutEngine.compute(albumId, width, bands);

        // label visibility: ONLY first layout shows it
        for (int i = 0; i < layouts.size(); i++) {
            layouts.get(i).showTimeLabel = (i == 0);
        }

        return layouts;
    }

    /* ================= Group Ordering ================= */

    private void insertGroupOrdered(Group g) {
        int idx = 0;
        while (idx < groups.size() && groupOrder(groups.get(idx).key) <= groupOrder(g.key))
            idx++;

        groups.add(idx, g);
        groupByKey.put(g.key, g);

        g.layoutStart = (idx == 0)
                ? 0
                : groups.get(idx - 1).layoutStart + groups.get(idx - 1).layoutCount;
        g.layoutCount = 0;

        shiftFollowingGroups(g, 0);
    }

    private void removeGroup(Group g) {
        groups.remove(g);
        groupByKey.remove(g.key);

        for (int i = 0; i < g.layoutCount; i++)
            flatLayouts.remove(g.layoutStart);

        shiftFollowingGroups(g, -g.layoutCount);
    }

    private void shiftFollowingGroups(Group base, int delta) {
        boolean shift = false;
        for (Group g : groups) {
            if (g == base) {
                shift = true;
                continue;
            }
            if (shift) g.layoutStart += delta;
        }
    }

    /* ================= Helpers ================= */

    private static void insertSorted(List<AlbumMedia> list, AlbumMedia m) {
        int i = 0;
        while (i < list.size() && list.get(i).momentMillis > m.momentMillis) i++;
        list.add(i, m);
    }

    private String bucketKey(AlbumMedia m) {
        TimeBucketizer.Bucket b = bucketizer.bucketOf(m.momentMillis);
        return bucketizer.keyOf(b, m.momentMillis);
    }

    private static int groupOrder(String k) {
        return switch (k) {
            case "today" -> 0;
            case "yesterday" -> 1;
            case "this_week" -> 2;
            case "this_month" -> 3;
            default -> {
                int y = Integer.parseInt(k.substring(0, 4));
                int m = Integer.parseInt(k.substring(5, 7));
                yield 10_000 - (y * 12 + m);
            }
        };
    }

    private void clear() {
        media.clear();
        flatLayouts.clear();
        groups.clear();
        groupByKey.clear();
    }

    /* ================= Group Model ================= */

    private static final class Group {
        final String key;
        final List<AlbumMedia> media = new ArrayList<>();
        List<BandLayout> layouts = List.of();
        int layoutStart;
        int layoutCount;

        Group(String key) { this.key = key; }
    }
}
