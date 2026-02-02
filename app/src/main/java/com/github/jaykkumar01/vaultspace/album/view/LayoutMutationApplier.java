package com.github.jaykkumar01.vaultspace.album.view;

import com.github.jaykkumar01.vaultspace.album.layout.BandLayout;

import java.util.List;

public final class LayoutMutationApplier {

    private final List<Group> groups;
    private final List<BandLayout> flatLayouts;

    public LayoutMutationApplier(
            List<Group> groups,
            List<BandLayout> flatLayouts
    ) {
        this.groups = groups;
        this.flatLayouts = flatLayouts;
    }

    /* ===== Public API ===== */

    public void insertGroup(
            Group group,
            List<BandLayout> layouts
    ) {
        if (layouts.isEmpty()) return;

        flatLayouts.addAll(group.layoutStart, layouts);
        shiftFollowingGroups(group, layouts.size());
    }

    public void removeGroup(
            Group group
    ) {
        int count = group.layoutCount;
        if (count == 0) return;

        int start = group.layoutStart;
        flatLayouts.subList(start, start + count).clear();
        shiftFollowingGroups(group, -count);
    }

    public void applyDiff(
            Group group,
            LayoutResult result,
            int delta
    ) {
        int removeCount = result.removeCount();
        int start = result.start();

        if (removeCount > 0)
            flatLayouts.subList(start, start + removeCount).clear();

        if (!result.items().isEmpty())
            flatLayouts.addAll(start, result.items());

        if (delta != 0)
            shiftFollowingGroups(group, delta);
    }

    /* ===== Internal ===== */

    private void shiftFollowingGroups(
            Group base,
            int delta
    ) {
        if (delta == 0) return;

        boolean shift = false;
        for (Group g : groups) {
            if (g == base) {
                shift = true;
                continue;
            }
            if (shift) g.layoutStart += delta;
        }
    }
}
