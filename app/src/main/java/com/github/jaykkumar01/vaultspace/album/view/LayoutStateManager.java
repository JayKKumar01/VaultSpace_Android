package com.github.jaykkumar01.vaultspace.album.view;

import com.github.jaykkumar01.vaultspace.album.band.TimeBucketizer;
import com.github.jaykkumar01.vaultspace.album.layout.BandLayout;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LayoutStateManager {

    /* ================= State ================= */

    private final List<AlbumMedia> media = new ArrayList<>();
    private final List<Group> groups = new ArrayList<>();
    private final Map<String, Group> groupByKey = new HashMap<>();
    private final List<BandLayout> flatLayouts = new ArrayList<>();

    private TimeBucketizer bucketizer;

    private final LayoutGroupBuilder groupBuilder = new LayoutGroupBuilder();
    private final LayoutDiffHelper diffHelper = new LayoutDiffHelper();
    private final LayoutMutationApplier mutator = new LayoutMutationApplier(groups, flatLayouts);

    /* ================= Public API ================= */

    public LayoutResult setMedia(String albumId, int width, List<AlbumMedia> sortedMedia) {
        clear();
        if (sortedMedia == null || sortedMedia.isEmpty()) return LayoutResult.setAll(flatLayouts);

        media.addAll(sortedMedia);
        bucketizer = TimeBucketizer.create(System.currentTimeMillis());

        for (AlbumMedia m : media) {
            TimeBucketizer.Result r = bucketizer.resolve(m.momentMillis);
            groupByKey.computeIfAbsent(r.key, k -> {
                Group g = new Group(k);
                groups.add(g);
                return g;
            }).media.add(m);
        }

        groups.sort((a, b) -> Integer.compare(groupOrder(a.key), groupOrder(b.key)));

        int cursor = 0;
        for (Group g : groups) {
            g.layoutStart = cursor;
            g.layouts = groupBuilder.build(albumId, width, g.media, bucketizer);
            g.layoutCount = g.layouts.size();
            flatLayouts.addAll(g.layouts);
            cursor += g.layoutCount;
        }

        return LayoutResult.setAll(flatLayouts);
    }

    public LayoutResult addMedia(String albumId, int width, AlbumMedia m) {
        if (m == null) return LayoutResult.replaceRange(0, 0, List.of());

        insertMediaSorted(m);

        TimeBucketizer.Result r = bucketizer().resolve(m.momentMillis);
        Group g = groupByKey.get(r.key);

        if (g == null) {
            g = new Group(r.key);
            g.media.add(m);
            insertGroupOrdered(g);

            List<BandLayout> layouts = groupBuilder.build(albumId, width, g.media, bucketizer);
            g.layouts = layouts;
            g.layoutCount = layouts.size();

            mutator.insertGroup(g, layouts);
            return LayoutResult.replaceRange(g.layoutStart, 0, layouts);
        }

        insertGroupMediaSorted(g, m);

        List<BandLayout> next = groupBuilder.build(albumId, width, g.media, bucketizer);
        LayoutResult diff = diffHelper.diff(g.layoutStart, g.layouts, next);

        mutator.applyDiff(g, diff, next.size() - g.layoutCount);
        g.layouts = next;
        g.layoutCount = next.size();

        return diff;
    }

    public LayoutResult removeMedia(String albumId, int width, AlbumMedia m) {
        if (m == null) return LayoutResult.replaceRange(0, 0, List.of());

        TimeBucketizer.Result r = bucketizer().resolve(m.momentMillis);
        Group g = groupByKey.get(r.key);
        if (g == null || !g.media.remove(m)) return LayoutResult.replaceRange(0, 0, List.of());

        media.remove(m);

        if (g.media.isEmpty()) {
            int start = g.layoutStart, count = g.layoutCount;
            mutator.removeGroup(g);
            groups.remove(g);
            groupByKey.remove(g.key);
            return LayoutResult.replaceRange(start, count, List.of());
        }

        List<BandLayout> next = groupBuilder.build(albumId, width, g.media, bucketizer);
        LayoutResult diff = diffHelper.diff(g.layoutStart, g.layouts, next);

        mutator.applyDiff(g, diff, next.size() - g.layoutCount);
        g.layouts = next;
        g.layoutCount = next.size();

        return diff;
    }

    /* ================= Helpers ================= */

    private TimeBucketizer bucketizer() {
        if (bucketizer == null) bucketizer = TimeBucketizer.create(System.currentTimeMillis());
        return bucketizer;
    }

    private void clear() {
        media.clear();
        groups.clear();
        groupByKey.clear();
        flatLayouts.clear();
    }

    private void insertMediaSorted(AlbumMedia m) {
        int idx = findInsertIndexDesc(media, m.momentMillis);
        media.add(idx, m);
    }

    private void insertGroupMediaSorted(Group g, AlbumMedia m) {
        int idx = findInsertIndexDesc(g.media, m.momentMillis);
        g.media.add(idx, m);
    }

    private static int findInsertIndexDesc(List<AlbumMedia> list, long momentMillis) {
        int lo = 0, hi = list.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (list.get(mid).momentMillis > momentMillis) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    private int groupOrder(String key) {
        return switch (key) {
            case "today" -> 0;
            case "yesterday" -> 1;
            case "this_week" -> 2;
            case "this_month" -> 3;
            default -> {
                int y = Integer.parseInt(key.substring(0, 4));
                int m = Integer.parseInt(key.substring(5, 7));
                yield 10_000 - (y * 12 + m);
            }
        };
    }

    private void insertGroupOrdered(Group g) {
        int idx = 0;
        while (idx < groups.size() && groupOrder(groups.get(idx).key) <= groupOrder(g.key)) idx++;

        groups.add(idx, g);
        groupByKey.put(g.key, g);

        g.layoutStart = (idx == 0) ? 0
                : groups.get(idx - 1).layoutStart + groups.get(idx - 1).layoutCount;
        g.layoutCount = 0;
    }
}
