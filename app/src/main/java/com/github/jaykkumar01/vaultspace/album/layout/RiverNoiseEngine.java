package com.github.jaykkumar01.vaultspace.album.layout;

import android.util.Log;

public final class RiverNoiseEngine {

    private static final String TAG = "VaultSpace:River";

    /* ============================================================
       Tuning — Flow Character
       ============================================================ */

    private static final float BASE_FREQUENCY = 0.22f;
    private static final float SEED_SCALE = 0.0001f;
    private static final float ENERGY_OFFSET = 37.7f;

    /* ============================================================
       Energy envelope
       ============================================================ */

    private static final float MIN_ENERGY = 0.6f;
    private static final float MAX_ENERGY = 1.4f;
    private static final float MAX_FLOW_ENERGY = 1.5f;

    /* ============================================================
       Rotation bounds (degrees)
       ============================================================ */

    private static final float MAX_ROTATION_PAIR = 1.4f;
    private static final float MAX_ROTATION_SOLO = 2.4f;

    private RiverNoiseEngine() {}

    /* ============================================================
       Public API
       ============================================================ */

    public static RiverTransform computeTransform(
            String albumId,
            String firstMediaId,
            int effectiveWidth,
            int usedWidth,
            boolean solo
    ) {
        /* ---------- Guards ---------- */
        if (albumId == null || firstMediaId == null ||
                effectiveWidth <= 0 || usedWidth <= 0) {
            return ZERO;
        }

        int freeSpace = effectiveWidth - usedWidth;
        if (freeSpace <= 0) return ZERO;

        int maxDrift = freeSpace / 2;
        if (maxDrift == 0) return ZERO;

        /* ---------- Deterministic Perlin domain ---------- */
        int bandKey = stableHash(albumId + ":" + firstMediaId);
        int seed = stableHash(albumId);

        // 🔑 Critical fix: spread hash into Perlin space
        float keyX = (bandKey & 0x7fffffff) % 10_000;

        float baseX =
                keyX * BASE_FREQUENCY
                        + seed * SEED_SCALE;

        /* ---------- Noise ---------- */
        float noise = perlin1D(baseX);
        float energyNoise = perlin1D(baseX + ENERGY_OFFSET);

        /* ---------- Energy ---------- */
        float spaceFactor =
                clamp(freeSpace / (float) effectiveWidth, 0f, 1f);

        float flowEnergy = clamp(
                lerp(MIN_ENERGY, MAX_ENERGY, energyNoise * 0.5f + 0.5f)
                        * lerp(0.8f, 1.2f, spaceFactor),
                MIN_ENERGY,
                MAX_FLOW_ENERGY
        );

        /* ---------- Drift ---------- */
        int xOffset = clamp(
                Math.round(noise * maxDrift * flowEnergy),
                -maxDrift,
                maxDrift
        );

        /* ---------- Rotation ---------- */
        float baseRotation = solo ? MAX_ROTATION_SOLO : MAX_ROTATION_PAIR;
        float rotationDeg =
                noise * baseRotation * lerp(0.6f, 1.0f, flowEnergy);

        Log.d(TAG,
                "bandKey=" + firstMediaId +
                        " noise=" + String.format("%.3f", noise) +
                        " x=" + xOffset +
                        " rot=" + String.format("%.2f", rotationDeg));

        return new RiverTransform(xOffset, rotationDeg);
    }

    /* ============================================================
       1D Perlin
       ============================================================ */

    private static float perlin1D(float x) {
        int x0 = fastFloor(x);
        int x1 = x0 + 1;

        float t = x - x0;

        float g0 = grad(x0);
        float g1 = grad(x1);

        float d0 = g0 * (x - x0);
        float d1 = g1 * (x - x1);

        return lerp(d0, d1, fade(t));
    }

    private static float grad(int x) {
        int h = x * 374761393;
        h = (h ^ (h >> 13)) * 1274126177;
        return ((h & 1) == 0) ? 1f : -1f;
    }

    private static float fade(float t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    private static int fastFloor(float x) {
        int i = (int) x;
        return x < i ? i - 1 : i;
    }

    /* ============================================================
       Utils
       ============================================================ */

    private static int stableHash(String s) {
        return s.hashCode();
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static final RiverTransform ZERO =
            new RiverTransform(0, 0f);
}
