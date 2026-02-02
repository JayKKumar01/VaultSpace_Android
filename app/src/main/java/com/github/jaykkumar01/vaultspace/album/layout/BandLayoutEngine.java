package com.github.jaykkumar01.vaultspace.album.layout;

import com.github.jaykkumar01.vaultspace.album.band.Band;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.album.model.AspectClass;
import com.github.jaykkumar01.vaultspace.album.model.MediaFrame;

import java.util.ArrayList;
import java.util.List;

public final class BandLayoutEngine {

    /* ================= Width ratios ================= */

    private static final float SOLO_WIDE      = 0.68f;
    private static final float SOLO_NEUTRAL   = 0.62f;
    private static final float SOLO_TALL      = 0.56f;
    private static final float SOLO_VERY_TALL = 0.40f;

    /* ================= Paired slack ================= */

    private static final float PAIR_SLACK_RATIO = 0.86f;

    /* ================= Spacing ================= */

    private static final int HORIZONTAL_GUTTER = 24;
    private static final int VERTICAL_PADDING  = 16;
    private static final int PAIR_GAP          = 12;

    /* ================= Height cap ================= */

    private static final int MAX_MEDIA_HEIGHT = 1700;

    /* ================= State ================= */

    private final String albumId;
    private final int effectiveWidth;

    /* ================= Constructor ================= */

    public BandLayoutEngine(String albumId, int riverWidth) {
        this.albumId = albumId;
        this.effectiveWidth = riverWidth - HORIZONTAL_GUTTER * 2;
    }

    /* ================= Public API ================= */

    public BandLayout compute(Band band) {
        BandLayout layout = band.isSolo() ? layoutSolo(band) : layoutPair(band);

        int usedWidth = computeUsedWidth(layout);
        RiverTransform t = RiverNoiseEngine.computeTransform(
                albumId,
                band.first.fileId,
                effectiveWidth,
                usedWidth,
                band.isSolo()
        );

        layout.rotationDeg = t.rotationDeg;
        for (MediaFrame f : layout.frames) f.baseX += t.xOffset;
        return layout;
    }

    public List<BandLayout> computeAll(List<Band> bands) {
        List<BandLayout> out = new ArrayList<>(bands.size());
        if (bands.isEmpty() || effectiveWidth <= 0) return out;
        for (Band band : bands) out.add(compute(band));
        return out;
    }

    /* ================= SOLO band ================= */

    private BandLayout layoutSolo(Band band) {
        AlbumMedia m = band.first;

        float ratio = switch (AspectClass.of(m)) {
            case WIDE -> SOLO_WIDE;
            case NEUTRAL -> SOLO_NEUTRAL;
            case TALL -> SOLO_TALL;
            default -> SOLO_VERY_TALL;
        };

        int width = (int) (effectiveWidth * ratio);
        int height = (int) (width / m.aspectRatio);

        int[] clamped = clampByMaxHeight(width, height, m.aspectRatio);
        width = clamped[0];
        height = clamped[1];

        int bandHeight = height + VERTICAL_PADDING * 2;
        int baseX = HORIZONTAL_GUTTER + (effectiveWidth - width) / 2;

        return new BandLayout(
                band.timeLabel,
                bandHeight,
                new MediaFrame[]{ new MediaFrame(m, width, height, baseX) }
        );
    }

    /* ================= PAIRED band ================= */

    private BandLayout layoutPair(Band band) {
        AlbumMedia a = band.first;
        AlbumMedia b = band.second;

        int usableWidth = (int) (effectiveWidth * PAIR_SLACK_RATIO);
        int pairWidth = (usableWidth - PAIR_GAP) / 2;

        int[] f1 = clampByMaxHeight(pairWidth, (int) (pairWidth / a.aspectRatio), a.aspectRatio);
        int[] f2 = clampByMaxHeight(pairWidth, (int) (pairWidth / b.aspectRatio), b.aspectRatio);

        int w1 = f1[0], h1 = f1[1];
        int w2 = f2[0], h2 = f2[1];

        int bandHeight = Math.max(h1, h2) + VERTICAL_PADDING * 2;
        int totalUsed = w1 + w2 + PAIR_GAP;
        int startX = HORIZONTAL_GUTTER + (effectiveWidth - totalUsed) / 2;

        return new BandLayout(
                band.timeLabel,
                bandHeight,
                new MediaFrame[]{
                        new MediaFrame(a, w1, h1, startX),
                        new MediaFrame(b, w2, h2, startX + w1 + PAIR_GAP)
                }
        );
    }

    /* ================= Helpers ================= */

    private static int[] clampByMaxHeight(int width, int height, float aspectRatio) {
        if (height <= MAX_MEDIA_HEIGHT) return new int[]{ width, height };
        int h = MAX_MEDIA_HEIGHT;
        int w = (int) (h * aspectRatio);
        return new int[]{ w, h };
    }

    private static int computeUsedWidth(BandLayout layout) {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        for (MediaFrame f : layout.frames) {
            minX = Math.min(minX, f.baseX);
            maxX = Math.max(maxX, f.baseX + f.width);
        }
        return maxX - minX;
    }
}
