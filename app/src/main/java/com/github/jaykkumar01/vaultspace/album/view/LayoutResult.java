package com.github.jaykkumar01.vaultspace.album.view;

import com.github.jaykkumar01.vaultspace.album.layout.BandLayout;

import java.util.List;

public final class LayoutResult {

    private final int start;
    private final int removeCount;
    private final List<BandLayout> items;

    private LayoutResult(int start, int removeCount, List<BandLayout> items) {
        this.start = start;
        this.removeCount = removeCount;
        this.items = items;
    }

    /* ===== Factories ===== */

    // used by setMedia → adapter.setAll(items)
    public static LayoutResult setAll(List<BandLayout> items) {
        return new LayoutResult(0, 0, items);
    }

    // used by add/remove → adapter.replaceRange(...)
    public static LayoutResult replaceRange(
            int start,
            int removeCount,
            List<BandLayout> items
    ) {
        return new LayoutResult(start, removeCount, items);
    }

    /* ===== Access ===== */

    public int start() {
        return start;
    }

    public int removeCount() {
        return removeCount;
    }

    public List<BandLayout> items() {
        return items;
    }
}
