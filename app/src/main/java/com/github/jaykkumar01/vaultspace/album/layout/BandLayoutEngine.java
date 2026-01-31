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
    private static final int VERTICAL_PADDING = 16;
    private static final int PAIR_GAP = 12;

    private BandLayoutEngine() {}

    /* ============================================================
       Public entry
       ============================================================ */

    public static List<BandLayout> compute(
            String albumId,
            int riverWidth,
            List<Band> bands
    ) {
        List<BandLayout> out = new ArrayList<>();
        if (bands.isEmpty() || riverWidth <= HORIZONTAL_GUTTER * 2) {
            return out;
        }

        int effectiveWidth = riverWidth - HORIZONTAL_GUTTER * 2;

        int bandIndex = 0;
        for (Band band : bands) {

            BandLayout layout = band.isSolo()
                    ? layoutSolo(effectiveWidth, band)
                    : layoutPair(effectiveWidth, band);

            int usedWidth = computeUsedWidth(layout);

            RiverTransform t = RiverNoiseEngine.computeTransform(
                    albumId,
                    bandIndex,
                    effectiveWidth,
                    usedWidth,
                    band.isSolo()
            );

            layout.rotationDeg = t.rotationDeg;

            for (MediaFrame f : layout.frames) {
                f.baseX += t.xOffset;
            }

            out.add(layout);
            bandIndex++;
        }

        return out;
    }

    /* ============================================================
       SOLO band
       ============================================================ */

    private static BandLayout layoutSolo(int effectiveWidth, Band band) {
        AlbumMedia m = band.first;

        float ratio = switch (AspectClass.of(m)) {
            case WIDE -> SOLO_WIDE;
            case NEUTRAL -> SOLO_NEUTRAL;
            case TALL -> SOLO_TALL;
            default -> SOLO_VERY_TALL;
        };

        int width = (int) (effectiveWidth * ratio);
        int height = (int) (width / m.aspectRatio);
        int bandHeight = height + VERTICAL_PADDING * 2;

        int baseX = HORIZONTAL_GUTTER + (effectiveWidth - width) / 2;

        return new BandLayout(
                band.timeLabel,
                bandHeight,
                new MediaFrame[]{
                        new MediaFrame(width, height, baseX)
                }
        );

    }

    /* ============================================================
       PAIRED band
       ============================================================ */

    private static BandLayout layoutPair(int effectiveWidth, Band band) {
        AlbumMedia a = band.first;
        AlbumMedia b = band.second;

        int usableWidth = (int) (effectiveWidth * PAIR_SLACK_RATIO);
        int pairWidth = (usableWidth - PAIR_GAP) / 2;

        int h1 = (int) (pairWidth / a.aspectRatio);
        int h2 = (int) (pairWidth / b.aspectRatio);

        int bandHeight = Math.max(h1, h2) + VERTICAL_PADDING * 2;

        int totalUsed = pairWidth * 2 + PAIR_GAP;
        int startX = HORIZONTAL_GUTTER + (effectiveWidth - totalUsed) / 2;

        return new BandLayout(
                band.timeLabel,
                bandHeight,
                new MediaFrame[]{
                        new MediaFrame(pairWidth, h1, startX),
                        new MediaFrame(pairWidth, h2, startX + pairWidth + PAIR_GAP)
                }
        );

    }

    /* ============================================================
       Utils
       ============================================================ */

    private static int computeUsedWidth(BandLayout layout) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;

        for (MediaFrame f : layout.frames) {
            minX = Math.min(minX, f.baseX);
            maxX = Math.max(maxX, f.baseX + f.width);
        }
        return maxX - minX;
    }
}
