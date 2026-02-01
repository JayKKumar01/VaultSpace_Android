package com.github.jaykkumar01.vaultspace.album.view;

import com.github.jaykkumar01.vaultspace.album.layout.BandLayout;

import java.util.List;

public final class LayoutResult {

    private final boolean full;
    private final int start;
    private final int removeCount;
    private final List<BandLayout> items;

    private LayoutResult(boolean full, int start, int removeCount, List<BandLayout> items) {
        this.full = full;
        this.start = start;
        this.removeCount = removeCount;
        this.items = items;
    }

    /* ===== Factories ===== */

    public static LayoutResult full(List<BandLayout> items) {
        return new LayoutResult(true, 0, 0, items);
    }

    public static LayoutResult range(
            int start,
            int removeCount,
            List<BandLayout> items
    ) {
        return new LayoutResult(false, start, removeCount, items);
    }

    /* ===== Access ===== */

    public boolean isFull() {
        return full;
    }

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
