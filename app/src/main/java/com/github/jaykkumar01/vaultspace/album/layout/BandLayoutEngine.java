package com.github.jaykkumar01.vaultspace.album.layout;

import com.github.jaykkumar01.vaultspace.album.band.Band;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.album.model.AspectClass;
import com.github.jaykkumar01.vaultspace.album.model.MediaFrame;

import java.util.ArrayList;
import java.util.List;

public final class BandLayoutEngine {

    /* ================= Width ratios ================= */

    private static final float SOLO_WIDE = 0.65f;
    private static final float SOLO_NEUTRAL = 0.60f;
    private static final float SOLO_TALL = 0.54f;
    private static final float SOLO_VERY_TALL = 0.34f;

    /* ================= Spacing ================= */

    private static final int HORIZONTAL_GUTTER = 24; // reserved for Perlin
    private static final int VERTICAL_PADDING = 16;  // river safety
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

        for (Band band : bands) {
            if (band.isSolo()) {
                out.add(layoutSolo(effectiveWidth, band));
            } else {
                out.add(layoutPair(effectiveWidth, band));
            }
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

        int width = (effectiveWidth - PAIR_GAP) / 2;

        int h1 = (int) (width / a.aspectRatio);
        int h2 = (int) (width / b.aspectRatio);

        int bandHeight = Math.max(h1, h2) + VERTICAL_PADDING * 2;

        int total = width * 2 + PAIR_GAP;
        int startX = HORIZONTAL_GUTTER + (effectiveWidth - total) / 2;

        return new BandLayout(
                band.timeLabel,
                bandHeight,
                new MediaFrame[]{
                        new MediaFrame(width, h1, startX),
                        new MediaFrame(width, h2, startX + width + PAIR_GAP)
                }
        );
    }
}
