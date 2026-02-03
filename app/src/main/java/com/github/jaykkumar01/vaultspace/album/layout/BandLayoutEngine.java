package com.github.jaykkumar01.vaultspace.album.layout;

import com.github.jaykkumar01.vaultspace.album.band.Band;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.album.model.AspectClass;
import com.github.jaykkumar01.vaultspace.album.model.MediaFrame;

import java.util.ArrayList;
import java.util.List;

public final class BandLayoutEngine {

    /* ================= Width ratios ================= */

    private static final float SOLO_WIDE = 0.66f;   // LANDSCAPE_WIDE
    private static final float SOLO_TALL = 0.54f;   // PORTRAIT_STRONG

    /* ================= Paired slack ================= */

    private static final float PAIR_SLACK_RATIO = 0.88f;

    /* ================= Spacing ================= */

    private static final int HORIZONTAL_GUTTER = 24;
    private static final int VERTICAL_PADDING  = 16;
    private static final int PAIR_GAP          = 12;

    private static final float MAX_ASPECT = 16f / 9f;

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

        AspectClass cls = AspectClass.of(m);
        int o = cls.ordinal(); // 0..6 (portrait → landscape)

        // Linear interpolation across the spectrum
        float t = o / 6f; // normalize
        float ratio = SOLO_TALL + t * (SOLO_WIDE - SOLO_TALL);

        float aspect = effectiveAspect(m.aspectRatio);

        int width  = Math.round(effectiveWidth * ratio);
        int height = Math.round(width / aspect);

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

        float aspectA = effectiveAspect(a.aspectRatio);
        float aspectB = effectiveAspect(b.aspectRatio);

        int usableWidth = Math.round(effectiveWidth * PAIR_SLACK_RATIO);
        int pairWidth = (usableWidth - PAIR_GAP) / 2;

        int h1 = Math.round(pairWidth / aspectA);
        int h2 = Math.round(pairWidth / aspectB);

        int bandHeight = Math.max(h1, h2) + VERTICAL_PADDING * 2;
        int totalUsed = pairWidth * 2 + PAIR_GAP;
        int startX = HORIZONTAL_GUTTER + (effectiveWidth - totalUsed) / 2;

        return new BandLayout(
                band.timeLabel,
                bandHeight,
                new MediaFrame[]{
                        new MediaFrame(a, pairWidth, h1, startX),
                        new MediaFrame(b, pairWidth, h2, startX + pairWidth + PAIR_GAP)
                }
        );
    }

    /* ================= Helpers ================= */

    private static float effectiveAspect(float r) {
        if (r > MAX_ASPECT) return MAX_ASPECT;
        return Math.max(r, 1f / MAX_ASPECT);
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
