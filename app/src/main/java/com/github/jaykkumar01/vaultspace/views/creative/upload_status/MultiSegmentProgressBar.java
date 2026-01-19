package com.github.jaykkumar01.vaultspace.views.creative.upload_status;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

public final class MultiSegmentProgressBar extends View {

    private static final long PROGRESS_DURATION = 180L;
    private static final int SWEEP_COLOR_INITIAL = 0x99FFFFFF;
    private static final int SWEEP_COLOR_FINAL = 0xFFFFFFFF;

    /* progress state */
    private float[] start = new float[0], target = new float[0], current = new float[0];
    private int[] colors = new int[0];

    private float targetSum;
    private boolean completionSweepConsumed;

    private boolean isAttached;
    private boolean isVisible = true;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sweepPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF rect = new RectF();
    private final Path clipPath = new Path();
    private float cornerRadius;

    private final FrameRunner frameRunner;
    private final ProgressTicker progressTicker;
    private final SweepTicker sweepTicker;

    public MultiSegmentProgressBar(Context c) {
        super(c);
        frameRunner = new FrameRunner(this, 45, this::onFrame);
        progressTicker = new ProgressTicker(PROGRESS_DURATION);
        sweepTicker = new SweepTicker(this);
        init();
    }

    public MultiSegmentProgressBar(Context c, AttributeSet a) {
        super(c, a);
        frameRunner = new FrameRunner(this, 45, this::onFrame);
        progressTicker = new ProgressTicker(PROGRESS_DURATION);
        sweepTicker = new SweepTicker(this);
        init();
    }

    private void init() {
        bgPaint.setStyle(Paint.Style.FILL);
        cornerRadius = getResources().getDisplayMetrics().density * 2f;
        syncBackgroundPaint();
    }

    /* ================= Public API ================= */

    public void setColors(int[] c) {
        colors = c != null ? c.clone() : new int[0];
        invalidate();
    }

    public void setFractions(@NonNull float[] f) {
        stopAll(true);
        ensureCapacity(f.length);

        float oldTargetSum = targetSum;
        targetSum = 0f;

        for (int i = 0; i < f.length; i++) {
            start[i] = current[i];
            target[i] = clamp01(f[i]);
            targetSum += target[i];
        }

        /* transition rules */
        if (oldTargetSum < 1f && targetSum >= 1f)
            completionSweepConsumed = false;

        if (targetSum == 0f)
            completionSweepConsumed = false;

        if (needsProgress())
            progressTicker.start(now());

        startEligibleAnimations();
    }

    /* ================= Lifecycle ================= */

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        isAttached = true;
        if (isVisible) startEligibleAnimations();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isAttached = false;
        stopAll(true);
    }

    @Override
    protected void onVisibilityChanged(View v, int visibility) {
        super.onVisibilityChanged(v, visibility);
        isVisible = visibility == VISIBLE;
        if (isVisible && isAttached) startEligibleAnimations();
        else frameRunner.stop();
    }

    /* ================= Frame ================= */

    private void onFrame(long now) {
        boolean dirty = false;

        if (progressTicker.isRunning()) {
            float t = progressTicker.tick(now);
            for (int i = 0; i < current.length; i++)
                current[i] = start[i] + (target[i] - start[i]) * t;
            dirty = true;
        }

        if (sweepTicker.isActive()) {
            if (!sweepTicker.tick(now)) {
                completionSweepConsumed = true;
                dirty = true;
            } else dirty = true;
        }

        if (dirty) invalidate();
        if (!hasActiveAnimations()) frameRunner.stop();
    }

    /* ================= Draw ================= */

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        rect.set(0, 0, w, h);
        clipPath.reset();
        clipPath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW);
    }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;

        int save = c.save();
        c.clipPath(clipPath);
        c.drawRect(0, 0, w, h, bgPaint);

        float x = 0f;
        for (int i = 0; i < current.length; i++) {
            float fw = current[i] * w;
            if (fw <= 0f) continue;
            fw = Math.min(fw, w - x);
            paint.setColor(i < colors.length ? colors[i] : 0);
            c.drawRect(x, 0, x + fw, h, paint);
            x += fw;
            if (x >= w) break;
        }

        float sx = sweepTicker.getSweepX();
        if (!Float.isNaN(sx)) {
            sweepPaint.setColor(sweepTicker.getColor());
            c.drawRect(sx, 0, sx + w * 0.45f, h, sweepPaint);
        }

        c.restoreToCount(save);
    }

    /* ================= Decisions ================= */

    private void startEligibleAnimations() {
        if (!isAttached || !isVisible) return;

        if (targetSum == 0f) {
            sweepTicker.startIdle(SWEEP_COLOR_INITIAL);
        } else if (targetSum >= 1f && !completionSweepConsumed) {
            sweepTicker.startCompletion(SWEEP_COLOR_FINAL);
        }

        if (hasActiveAnimations())
            frameRunner.start();
    }

    private boolean hasActiveAnimations() {
        return progressTicker.isRunning() || sweepTicker.isActive();
    }

    private boolean needsProgress() {
        for (int i = 0; i < current.length; i++)
            if (current[i] != target[i]) return true;
        return false;
    }

    private void stopAll(boolean clearSweep) {
        frameRunner.stop();
        progressTicker.stop();
        if (clearSweep) sweepTicker.stop();
    }

    /* ================= Helpers ================= */

    private void ensureCapacity(int n) {
        if (n == current.length) return;
        start = new float[n];
        target = new float[n];
        current = new float[n];
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }

    private static long now() {
        return android.os.SystemClock.uptimeMillis();
    }

    private void syncBackgroundPaint() {
        if (getBackground() instanceof android.graphics.drawable.ColorDrawable)
            bgPaint.setColor(((android.graphics.drawable.ColorDrawable) getBackground()).getColor());
    }
}
