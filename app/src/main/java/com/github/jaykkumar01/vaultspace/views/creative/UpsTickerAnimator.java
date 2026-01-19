package com.github.jaykkumar01.vaultspace.views.creative;

import android.os.SystemClock;
import android.view.View;
import androidx.annotation.NonNull;

/**
 * A lightweight, UPS-limited, time-based animator.
 *
 * - No ValueAnimator
 * - Time-driven (device independent)
 * - UPS capped (foreground / background)
 * - Pauses automatically based on visibility & attachment
 *
 * Owner (usually a View) must forward lifecycle signals.
 */
public final class UpsTickerAnimator {

    public interface FrameCallback {
        /**
         * @param progress normalized [0..1)
         */
        void onFrame(float progress);
    }

    /* ================= Config ================= */

    private final long durationMs;
    private final long frameIntervalFgMs;
    private final long frameIntervalBgMs;

    /* ================= State ================= */

    private boolean running;
    private boolean isAttached;
    private boolean isVisible = true;
    private boolean isForeground = true;

    private boolean oneShot;
    private long startMs;
    private long lastTickMs;

    private final View host;
    private final FrameCallback callback;

    /* ================= Runnable ================= */

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (!running) return;

            long now = SystemClock.uptimeMillis();
            long frameInterval = isForeground ? frameIntervalFgMs : frameIntervalBgMs;

            if (lastTickMs != 0L) {
                long delta = now - lastTickMs;
                if (delta < frameInterval) {
                    host.postDelayed(this, frameInterval - delta);
                    return;
                }
            }

            lastTickMs = now;

            long elapsed = now % durationMs;
            float progress = elapsed / (float) durationMs;

            callback.onFrame(progress);

            if (oneShot) {
                long totalElapsed = now - startMs;
                if (totalElapsed >= durationMs) {
                    stop();
                    return;
                }
            }

            host.postDelayed(this, frameInterval);
        }
    };

    /* ================= Constructor ================= */

    public UpsTickerAnimator(
            @NonNull View host,
            long durationMs,
            int upsForeground,
            int upsBackground,
            @NonNull FrameCallback callback
    ) {
        this.host = host;
        this.durationMs = durationMs;
        this.frameIntervalFgMs = 1000L / upsForeground;
        this.frameIntervalBgMs = 1000L / upsBackground;
        this.callback = callback;
    }

    /* ================= Control ================= */

    public void start() {
        if (running) return;
        running = true;
        lastTickMs = 0L;
        startMs = SystemClock.uptimeMillis();
        host.removeCallbacks(ticker);
        host.post(ticker);
    }

    public void stop() {
        running = false;
        lastTickMs = 0L;
        startMs = 0L;
        host.removeCallbacks(ticker);
    }

    /* ================= Optional behavior ================= */

    public void setOneShot(boolean oneShot) {
        this.oneShot = oneShot;
    }

    /* ================= Lifecycle signals ================= */

    public void onAttached() {
        isAttached = true;
        evaluate();
    }

    public void onDetached() {
        isAttached = false;
        stop();
    }

    public void onVisibilityChanged(boolean visible) {
        isVisible = visible;
        evaluate();
    }

    public void onAppForeground() {
        isForeground = true;
        evaluate();
    }

    public void onAppBackground() {
        isForeground = false;
        evaluate();
    }

    /* ================= Internal ================= */

    private void evaluate() {
        if (!isAttached || !isVisible) {
            stop();
        } else {
            start();
        }
    }
}
