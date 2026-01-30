package com.github.jaykkumar01.vaultspace.album.layout;

import com.github.jaykkumar01.vaultspace.album.model.AlbumItem;
import com.github.jaykkumar01.vaultspace.album.model.Band;

import java.util.ArrayList;
import java.util.List;

public final class AlbumLayoutEngine {

    private static final int SIDE_PAD = 16, INNER_GAP = 16, LABEL_H = 24, VBREATH = 16;
    private static final int BASE_W = 140, PAIR_GUTTER = 16;

    private static final float MAX_OFF_X = 6f, MAX_OFF_Y = 4f, MAX_ROT = 2.5f;
    private static final float STEP = 0.21f;

    private AlbumLayoutEngine() {
    }

    public static List<ResolvedBandLayout> resolve(String albumId, int screenW, List<Band> bands) {
        List<ResolvedBandLayout> out = new ArrayList<>(bands.size());
        float seed = seed(albumId);

        for (Band b : bands) {
            float t = seed + b.bandIndex * STEP;
            float n0 = noise(t), n1 = noise(t + 11.3f), n2 = noise(t + 23.7f), n3 = noise(t + 41.9f);

            ResolvedBandLayout r;
            if (b.isSolo()) {
                r = layoutSolo(b, screenW, n0, n1, n2, n3);
            } else {
                r = layoutPair(b, screenW, n0, n1, n2);
            }
            assertBand(screenW, r);
            out.add(r);

        }
        return out;
    }

    /* ================= SOLO ================= */

    private static ResolvedBandLayout layoutSolo(
            Band b, int screenW, float xB, float yB, float rB, float wB
    ) {
        AlbumItem it = b.items.get(0);
        float ar = it.media.aspectRatio;

        float baseW = BASE_W;
        if (ar > 1.3f) baseW *= 1.25f;
        else if (ar < 0.7f) baseW *= 0.85f;
        else if (ar < 0.9f) baseW *= 0.95f;

        float scale = clamp(1f + wB * 0.15f, 0.85f, 1.15f);
        int w = Math.round(baseW * scale);
        int h = Math.round(w / ar);

        int cx = screenW / 2;
        float maxDrift = (screenW - w) * 0.12f;
        int x = Math.round(cx - w / 2f + xB * maxDrift);
        x = clamp(x, SIDE_PAD, screenW - SIDE_PAD - w);

        int y = LABEL_H + VBREATH + Math.round(yB * VBREATH);

        float ox = xB * MAX_OFF_X, oy = yB * MAX_OFF_Y, rot = rB * MAX_ROT;

        int bandH = LABEL_H + VBREATH * 2 + h + Math.round(Math.abs(oy));

        ResolvedItemFrame f = new ResolvedItemFrame(
                w, h, x, y, ox, oy, rot, it.media.momentMillis
        );
        return new ResolvedBandLayout(b.bandIndex, bandH, List.of(f));
    }

    /* ================= PAIR ================= */

    private static ResolvedBandLayout layoutPair(
            Band b, int screenW, float xB, float yB, float rB
    ) {
        int avail = screenW - SIDE_PAD * 2 - PAIR_GUTTER;
        int colW = avail / 2;

        AlbumItem a = b.items.get(0), c = b.items.get(1);

        int w0 = Math.min(BASE_W, colW);
        int w1 = Math.min(BASE_W, colW);

        float dom = 0.08f * xB;
        w0 = Math.round(w0 * (1f + dom));
        w1 = Math.round(w1 * (1f - dom));

        int h0 = Math.round(w0 / a.media.aspectRatio);
        int h1 = Math.round(w1 / c.media.aspectRatio);
        int maxH = Math.max(h0, h1);

        int x1 = SIDE_PAD + colW + PAIR_GUTTER;

        int y0 = LABEL_H + VBREATH + (maxH - h0) / 2;
        int y1 = LABEL_H + VBREATH + (maxH - h1) / 2;

        float ox = xB * MAX_OFF_X, oy = yB * MAX_OFF_Y, rot = rB * MAX_ROT;

        ResolvedItemFrame f0 = new ResolvedItemFrame(
                w0, h0, SIDE_PAD, y0, ox, oy, rot, a.media.momentMillis
        );
        ResolvedItemFrame f1 = new ResolvedItemFrame(
                w1, h1, x1, y1, ox, -oy, -rot, c.media.momentMillis
        );

        int bandH = LABEL_H + VBREATH * 2 + maxH + Math.round(Math.abs(oy));
        return new ResolvedBandLayout(b.bandIndex, bandH, List.of(f0, f1));
    }

    /* ================= Noise ================= */

    private static float seed(String s) {
        int h = 0;
        for (int i = 0; i < s.length(); i++) h = 31 * h + s.charAt(i);
        return (h & 0xFFFF) / 1000f;
    }

    private static float noise(float x) {
        int xi = (int) Math.floor(x) & 255;
        float xf = x - (int) Math.floor(x);
        float u = xf * xf * xf * (xf * (xf * 6 - 15) + 10);
        int a = PERM[xi], b = PERM[xi + 1];
        return lerp(grad(a, xf), grad(b, xf - 1), u);
    }

    private static float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    private static float grad(int h, float x) {
        return ((h & 1) == 0) ? x : -x;
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static final int[] PERM = new int[512];

    static {
        int[] p = {
                151, 160, 137, 91, 90, 15, 131, 13, 201, 95, 96, 53, 194, 233, 7, 225,
                140, 36, 103, 30, 69, 142, 8, 99, 37, 240, 21, 10, 23,
                190, 6, 148, 247, 120, 234, 75, 0, 26, 197, 62, 94, 252, 219, 203, 117,
                35, 11, 32, 57, 177, 33, 88, 237, 149, 56, 87, 174, 20, 125, 136, 171,
                168, 68, 175, 74, 165, 71, 134, 139, 48, 27, 166, 77, 146, 158, 231, 83,
                111, 229, 122, 60, 211, 133, 230, 220, 105, 92, 41, 55, 46, 245, 40, 244,
                102, 143, 54, 65, 25, 63, 161, 1, 216, 80, 73, 209, 76, 132, 187, 208,
                89, 18, 169, 200, 196, 135, 130, 116, 188, 159, 86, 164, 100, 109, 198, 173,
                186, 3, 64, 52, 217, 226, 250, 124, 123, 5, 202, 38, 147, 118, 126, 255,
                82, 85, 212, 207, 206, 59, 227, 47, 16, 58, 17, 182, 189, 28, 42, 223,
                183, 170, 213, 119, 248, 152, 2, 44, 154, 163, 70, 221, 153, 101, 155, 167,
                43, 172, 9, 129, 22, 39, 253, 19, 98, 108, 110, 79, 113, 224, 232, 178,
                185, 112, 104, 218, 246, 97, 228, 251, 34, 242, 193, 238, 210, 144, 12, 191,
                179, 162, 241, 81, 51, 145, 235, 249, 14, 239, 107, 49, 192, 214, 31, 181,
                199, 106, 157, 184, 84, 204, 176, 115, 121, 50, 45, 127, 4, 150, 254, 138,
                236, 205, 93, 222, 114, 67, 29, 24, 72, 243, 141, 128, 195, 78, 66, 215
        };
        for (int i = 0; i < 512; i++) PERM[i] = p[i % p.length];
    }

    /* ================= Debug Assertions ================= */

    private static void assertBand(
            int screenW, ResolvedBandLayout band
    ) {
        if (band.bandHeightPx <= 0)
            throw new IllegalStateException("bandHeight<=0 idx=" + band.bandIndex);

        for (int i = 0; i < band.items.size(); i++) {
            ResolvedItemFrame f = band.items.get(i);

            if (f.widthPx <= 0 || f.heightPx <= 0)
                throw new IllegalStateException("size<=0 band=" + band.bandIndex + " item=" + i);

            if (f.xPx < 0 || f.xPx + f.widthPx > screenW)
                throw new IllegalStateException("x overflow band=" + band.bandIndex + " item=" + i);

            if (f.yPx < 0)
                throw new IllegalStateException("y<0 band=" + band.bandIndex + " item=" + i);

            float bottom = f.yPx + f.heightPx + Math.abs(f.offsetYPx);
            if (bottom > band.bandHeightPx)
                throw new IllegalStateException(
                        "y overflow band=" + band.bandIndex +
                                " item=" + i +
                                " bottom=" + bottom +
                                " bandH=" + band.bandHeightPx
                );
        }
    }

}
