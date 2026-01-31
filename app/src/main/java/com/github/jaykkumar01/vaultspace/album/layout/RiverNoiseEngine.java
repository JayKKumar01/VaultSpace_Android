package com.github.jaykkumar01.vaultspace.album.layout;

import android.util.Log;

public final class RiverNoiseEngine {

    private static final String TAG = "VaultSpace:River";

    /* ============================================================
       Tuning — Flow Character
       ============================================================ */

    // Higher frequency → faster bends (full-paced river)
    private static final float BASE_FREQUENCY = 0.22f;

    // Stable album seed influence
    private static final float SEED_SCALE = 0.0001f;

    // Secondary noise offset for energy modulation
    private static final float ENERGY_OFFSET = 37.7f;

    /* ============================================================
       Energy envelope
       ============================================================ */

    private static final float MIN_ENERGY = 0.6f;
    private static final float MAX_ENERGY = 1.4f;

    // 🔒 Absolute safety cap (prevents overflow on ultra-wide layouts)
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

    public static RiverTransform computeTransform(String albumId, int bandIndex, int effectiveWidth, int usedWidth, boolean solo) {
        /* ---------- Hard guards ---------- */
        if (albumId == null || effectiveWidth <= 0 || usedWidth <= 0) {
            return ZERO;
        }

        int freeSpace = effectiveWidth - usedWidth;
        if (freeSpace <= 0) {
            log(bandIndex, freeSpace, 0, 0f, 0, 0f);
            return ZERO;
        }

        int maxDrift = freeSpace / 2;
        if (maxDrift == 0) {
            log(bandIndex, freeSpace, maxDrift, 0f, 0, 0f);
            return ZERO;
        }

        /* ---------- Deterministic Perlin domain ---------- */
        int seed = stableHash(albumId);
        float baseX =
                bandIndex * BASE_FREQUENCY
                        + seed * SEED_SCALE;

        /* ---------- Primary direction noise ---------- */
        float noise = perlin1D(baseX); // [-1, +1]

        /* ---------- Secondary energy noise ---------- */
        float energyNoise = perlin1D(baseX + ENERGY_OFFSET); // [-1, +1]

        /* ---------- Flow energy (bounded & safe) ---------- */
        float spaceFactor = clamp(
                freeSpace / (float) effectiveWidth,
                0f,
                1f
        );

        float energyFromNoise =
                lerp(MIN_ENERGY, MAX_ENERGY, energyNoise * 0.5f + 0.5f);

        float energyFromSpace =
                lerp(0.8f, 1.2f, spaceFactor);

        float flowEnergy = energyFromNoise * energyFromSpace;

        // 🔒 Hard safety clamp
        flowEnergy = clamp(flowEnergy, MIN_ENERGY, MAX_FLOW_ENERGY);

        /* ---------- Horizontal drift (fast, expressive, but guaranteed safe) ---------- */
        int rawOffset = Math.round(noise * maxDrift * flowEnergy);
        int xOffset = clamp(rawOffset, -maxDrift, maxDrift);

        /* ---------- Rotation (expressive but visually controlled) ---------- */
        float baseRotation = solo ? MAX_ROTATION_SOLO : MAX_ROTATION_PAIR;
        float rotationDeg =
                noise * baseRotation * lerp(0.6f, 1.0f, flowEnergy);

        log(bandIndex, freeSpace, maxDrift, noise, xOffset, rotationDeg);

        return new RiverTransform(xOffset, rotationDeg);
    }

    /* ============================================================
       1D Perlin (deterministic)
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
       Logging
       ============================================================ */

    private static void log(
            int bandIndex,
            int freeSpace,
            int maxDrift,
            float noise,
            int xOffset,
            float rotationDeg
    ) {
        Log.d(TAG,
                "band=" + bandIndex +
                        " free=" + freeSpace +
                        " max=" + maxDrift +
                        " noise=" + String.format("%.3f", noise) +
                        " x=" + xOffset +
                        " rot=" + String.format("%.2f", rotationDeg)
        );
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

    private static final RiverTransform ZERO = new RiverTransform(0, 0f);
}
