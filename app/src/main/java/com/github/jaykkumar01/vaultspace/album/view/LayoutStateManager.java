package com.github.jaykkumar01.vaultspace.album.view;

import com.github.jaykkumar01.vaultspace.album.band.Band;
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
    private final Map<String, AlbumMedia> mediaById = new HashMap<>();

    private final List<Group> groups = new ArrayList<>();
    private final Map<String, Group> groupByKey = new HashMap<>();
    private final List<BandLayout> flatLayouts = new ArrayList<>();

    private TimeBucketizer bucketizer;

    private final LayoutGroupBuilder groupBuilder = new LayoutGroupBuilder();
    private final BandDiffHelper bandDiffHelper = new BandDiffHelper();
    private final LayoutMutationApplier mutator = new LayoutMutationApplier(groups, flatLayouts);

    /* ================= Public API ================= */

    public LayoutResult setMedia(String albumId, int width, List<AlbumMedia> sortedMedia) {
        clear();
        if (sortedMedia == null || sortedMedia.isEmpty()) return LayoutResult.setAll(flatLayouts);

        media.addAll(sortedMedia);
        bucketizer = TimeBucketizer.create(System.currentTimeMillis());

        for (AlbumMedia m : media) {
            mediaById.put(m.fileId, m);
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
            g.bands = groupBuilder.buildBands(g.media, bucketizer);
            g.layouts = groupBuilder.buildLayouts(albumId, width, g.bands);
            g.layoutCount = g.layouts.size();

            flatLayouts.addAll(g.layouts);
            if (g.layoutCount > 0) flatLayouts.get(g.layoutStart).showTimeLabel = true;

            cursor += g.layoutCount;
        }

        return LayoutResult.setAll(flatLayouts);
    }

    public LayoutResult addMedia(String albumId, int width, AlbumMedia m) {
        if (m == null) return LayoutResult.replaceRange(0, 0, List.of());
        mediaById.put(m.fileId, m);

        insertMediaSorted(m);

        TimeBucketizer.Result r = bucketizer().resolve(m.momentMillis);
        Group g = groupByKey.get(r.key);

        if (g == null) {
            g = new Group(r.key);
            g.media.add(m);
            insertGroupOrdered(g);

            g.bands = groupBuilder.buildBands(g.media, bucketizer);
            g.layouts = groupBuilder.buildLayouts(albumId, width, g.bands);
            g.layoutCount = g.layouts.size();

            mutator.insertGroup(g, g.layouts);
            if (g.layoutCount > 0) flatLayouts.get(g.layoutStart).showTimeLabel = true;
            return LayoutResult.replaceRange(g.layoutStart, 0, g.layouts);
        }

        insertGroupMediaSorted(g, m);

        List<Band> nextBands = groupBuilder.buildBands(g.media, bucketizer);
        BandDiff d = bandDiffHelper.diff(g.bands, nextBands);

        if (d.removeCount == 0 && d.items.isEmpty()) {
            g.bands = nextBands;
            if (g.layoutCount > 0) flatLayouts.get(g.layoutStart).showTimeLabel = true;
            return LayoutResult.replaceRange(0, 0, List.of());
        }

        List<BandLayout> inserted = groupBuilder.buildLayouts(albumId, width, d.items);

        LayoutResult rlt = LayoutResult.replaceRange(g.layoutStart + d.start, d.removeCount, inserted);

        mutator.applyDiff(g, rlt, inserted.size() - d.removeCount);
        if (g.layoutCount > 0) flatLayouts.get(g.layoutStart).showTimeLabel = true;


        g.bands = nextBands;
        g.layoutCount += inserted.size() - d.removeCount;
        g.layouts = flatLayouts.subList(g.layoutStart, g.layoutStart + g.layoutCount);

        return rlt;
    }


    public LayoutResult removeMedia(String albumId, int width, String mediaId) {
        AlbumMedia m = mediaById.get(mediaId);
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

            if (start < flatLayouts.size())
                flatLayouts.get(start).showTimeLabel = true;

            mediaById.remove(mediaId); // ✅ added
            return LayoutResult.replaceRange(start, count, List.of());
        }

        List<Band> nextBands = groupBuilder.buildBands(g.media, bucketizer);
        BandDiff d = bandDiffHelper.diff(g.bands, nextBands);

        if (d.removeCount == 0 && d.items.isEmpty()) {
            g.bands = nextBands;
            if (g.layoutCount > 0) flatLayouts.get(g.layoutStart).showTimeLabel = true;

            mediaById.remove(mediaId); // ✅ added
            return LayoutResult.replaceRange(0, 0, List.of());
        }

        List<BandLayout> inserted = groupBuilder.buildLayouts(albumId, width, d.items);

        LayoutResult rlt = LayoutResult.replaceRange(g.layoutStart + d.start, d.removeCount, inserted);

        mutator.applyDiff(g, rlt, inserted.size() - d.removeCount);
        if (g.layoutCount > 0) flatLayouts.get(g.layoutStart).showTimeLabel = true;

        g.bands = nextBands;
        g.layoutCount += inserted.size() - d.removeCount;
        g.layouts = flatLayouts.subList(g.layoutStart, g.layoutStart + g.layoutCount);

        mediaById.remove(mediaId); // ✅ added
        return rlt;
    }



    /* ================= Helpers ================= */


    private TimeBucketizer bucketizer() {
        if (bucketizer == null) bucketizer = TimeBucketizer.create(System.currentTimeMillis());
        return bucketizer;
    }

    private void clear() {
        media.clear();
        mediaById.clear();   // ✅ REQUIRED
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
