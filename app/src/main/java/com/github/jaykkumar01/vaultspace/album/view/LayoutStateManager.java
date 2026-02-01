package com.github.jaykkumar01.vaultspace.album.view;

import androidx.annotation.NonNull;

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
            groupByKey.computeIfAbsent(key, k -> {
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

    public LayoutResult addMedia(String albumId,int width,AlbumMedia m) {
        if (m == null) return LayoutResult.full(flatLayouts);

        insertSorted(media, m);
        String key = bucketKey(m);
        Group g = groupByKey.get(key);

        if (g == null) {
            g = new Group(key);
            g.media.add(m);
            insertGroupOrdered(g);
            GroupRebuildResult r = rebuildGroup(albumId, width, g, true);
            return LayoutResult.range(r.adapterStart, r.removeCount, r.insertItems);
        }

        insertSorted(g.media, m);
        GroupRebuildResult r = rebuildGroup(albumId, width, g, false);
        return LayoutResult.range(r.adapterStart, r.removeCount, r.insertItems);
    }


    public LayoutResult removeMedia(String albumId,int width,AlbumMedia m) {
        if (m == null) return LayoutResult.full(flatLayouts);

        String key = bucketKey(m);
        Group g = groupByKey.get(key);
        if (g == null || !g.media.remove(m))
            return LayoutResult.full(flatLayouts);

        media.remove(m);

        // group becomes empty → full removal
        if (g.media.isEmpty()) {
            int oldCount = g.layoutCount;
            removeGroup(g);
            return LayoutResult.range(g.layoutStart, oldCount, List.of());
        }

        // symmetric with addMedia
        GroupRebuildResult r = rebuildGroup(albumId, width, g, false);
        return LayoutResult.range(r.adapterStart, r.removeCount, r.insertItems);
    }


    /* ================= Group Rebuild ================= */

    private GroupRebuildResult rebuildGroup(String albumId,int width,Group g,boolean isNew) {
        List<BandLayout> old = g.layouts;
        List<BandLayout> next = buildGroupLayouts(albumId, width, g);

        if (isNew) {
            g.layouts = next;
            g.layoutCount = next.size();
            flatLayouts.addAll(g.layoutStart, next);
            shiftFollowingGroups(g, next.size());
            return new GroupRebuildResult(g.layoutStart, 0, next);
        }

        GroupDiff diff = diffLayouts(old, next);
        android.util.Log.d("LayoutDiff", "group=" + g.key + " " + diff);

        // FULL replace fallback
        if (diff.sameStart == 0 && diff.sameEnd == 0) {
            for (int i = 0; i < old.size(); i++)
                flatLayouts.remove(g.layoutStart);
            flatLayouts.addAll(g.layoutStart, next);

            g.layouts = next;
            g.layoutCount = next.size();
            shiftFollowingGroups(g, next.size() - old.size());

            return new GroupRebuildResult(
                    g.layoutStart,
                    old.size(),
                    next
            );
        }

        // PARTIAL replace (prefix + suffix preserved)
        int replaceStartInGroup = diff.sameStart;
        int removeCount = old.size() - diff.sameStart - diff.sameEnd;
        int insertFrom = diff.sameStart;
        int insertTo = next.size() - diff.sameEnd;

        int flatStart = g.layoutStart + replaceStartInGroup;

        for (int i = 0; i < removeCount; i++)
            flatLayouts.remove(flatStart);

        List<BandLayout> insertItems = next.subList(insertFrom, insertTo);
        flatLayouts.addAll(flatStart, insertItems);

        g.layouts = next;
        g.layoutCount = next.size();
        shiftFollowingGroups(g, next.size() - old.size());

        return new GroupRebuildResult(
                flatStart,
                removeCount,
                insertItems
        );
    }



    private static GroupDiff diffLayouts(List<BandLayout> old, List<BandLayout> next) {
        int oldSize = old.size(), newSize = next.size();

        if (oldSize == 0 || newSize == 0)
            return new GroupDiff(0, 0, oldSize, newSize);

        int sameStart = 0;
        int max = Math.min(oldSize, newSize);
        while (sameStart < max && old.get(sameStart).equals(next.get(sameStart)))
            sameStart++;

        int sameEnd = 0;
        int oi = oldSize - 1, ni = newSize - 1;
        while (oi >= sameStart && ni >= sameStart &&
                old.get(oi).equals(next.get(ni))) {
            sameEnd++;
            oi--;
            ni--;
        }

        return new GroupDiff(sameStart, sameEnd, oldSize, newSize);
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

        Group(String key) {
            this.key = key;
        }
    }

    public static final class GroupDiff {
        final int sameStart;
        final int sameEnd;
        final int oldSize;
        final int newSize;

        GroupDiff(int sameStart,int sameEnd,int oldSize,int newSize) {
            this.sameStart = sameStart;
            this.sameEnd = sameEnd;
            this.oldSize = oldSize;
            this.newSize = newSize;
        }

        boolean isFullReplace() {
            return sameStart == 0 && sameEnd == 0;
        }

        int replaceStart() {
            return sameStart;
        }

        int removeCount() {
            return oldSize - sameStart - sameEnd;
        }

        int insertFrom() {
            return sameStart;
        }

        int insertTo() {
            return newSize - sameEnd;
        }

        @NonNull
        @Override
        public String toString() {
            return "old=" + oldSize +
                    " new=" + newSize +
                    " sameStart=" + sameStart +
                    " sameEnd=" + sameEnd +
                    " replaceStart=" + replaceStart() +
                    " remove=" + removeCount();
        }
    }


    private record GroupRebuildResult(int adapterStart, int removeCount, List<BandLayout> insertItems){}

}
