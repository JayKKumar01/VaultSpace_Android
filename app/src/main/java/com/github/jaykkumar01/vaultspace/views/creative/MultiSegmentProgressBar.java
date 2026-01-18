package com.github.jaykkumar01.vaultspace.views.creative;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

public final class MultiSegmentProgressBar extends View {

    private static final long DEFAULT_DURATION = 180L;
    private static final long SWEEP_DURATION = 2000L;

    private static final int SWEEP_COLOR_INITIAL = 0x99FFFFFF; // greyed white
    private static final int SWEEP_COLOR_FINAL = 0xFFFFFFFF; // pure white


    private float[] target = new float[0];
    private float[] animated = new float[0];
    private float[] start = new float[0];
    private int[] colors = new int[0];

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sweepPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path clipPath = new Path();
    private final RectF rect = new RectF();
    private float cornerRadius;

    private long duration = DEFAULT_DURATION;
    private TimeInterpolator interpolator = new FastOutSlowInInterpolator();

    private long animStart;
    private boolean animating;
    private ValueAnimator animator;
    private ValueAnimator sweepAnimator;

    private float sweepX = Float.NaN;
    private boolean completionPlayed;

    public MultiSegmentProgressBar(Context c) {
        super(c);
        init();
    }

    public MultiSegmentProgressBar(Context c, @Nullable AttributeSet a) {
        super(c, a);
        init();
    }

    private void init() {
        bgPaint.setStyle(Paint.Style.FILL);
        sweepPaint.setColor(0xFFFFFFFF);
        cornerRadius = getResources().getDisplayMetrics().density * 2f;
        syncBackgroundPaint();
    }

    /* ================= Public API ================= */

    public void setColors(int[] colors) {
        this.colors = colors != null ? colors.clone() : new int[0];
        invalidate();
    }

    public void setFractions(float[] fractions) {
        resetTransientState();
        ensureCapacity(fractions);
        for (int i = 0; i < fractions.length; i++) {
            target[i] = clamp01(fractions[i]);
        }
        startAnimationIfNeeded();
    }

    public void setFractionsImmediate(float[] fractions) {
        resetTransientState();
        ensureCapacity(fractions);
        for (int i = 0; i < fractions.length; i++) {
            float v = clamp01(fractions[i]);
            target[i] = v;
            animated[i] = v;
            start[i] = v;
        }
        maybeStartSweep(); // ← important for immediate empty / full
        invalidate();
    }

    public void setAnimationDuration(long millis) {
        duration = Math.max(0, millis);
    }

    public void setInterpolator(@Nullable TimeInterpolator i) {
        interpolator = i != null ? i : new FastOutSlowInInterpolator();
    }

    /* ================= Fraction animation ================= */

    private void startAnimationIfNeeded() {
        boolean diff = false;
        for (int i = 0; i < target.length; i++) {
            if (animated[i] != target[i]) {
                diff = true;
                break;
            }
        }
        if (!diff) {
            maybeStartSweep();
            return;
        }

        System.arraycopy(animated, 0, start, 0, animated.length);
        animStart = System.currentTimeMillis();
        animating = true;

        if (animator == null) {
            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.addUpdateListener(a -> onAnimFrame());
        }
        animator.setInterpolator(interpolator);
        animator.setDuration(duration);
        animator.start();
    }

    private void onAnimFrame() {
        float t = (System.currentTimeMillis() - animStart) / (float) duration;
        t = clamp01(interpolator.getInterpolation(t));

        boolean done = true;
        for (int i = 0; i < animated.length; i++) {
            float v = lerp(start[i], target[i], t);
            animated[i] = v;
            if (v != target[i]) done = false;
        }

        invalidate();

        if (done) {
            animating = false;
            animator.cancel();
            maybeStartSweep();
        }
    }

    /* ================= Sweep logic ================= */

    private void maybeStartSweep() {
        if (completionPlayed || animating) return;

        float sum = 0f;
        boolean allZero = true;
        for (float v : target) {
            sum += v;
            if (v > 0f) allZero = false;
        }

        if (!allZero && sum < 0.999f) return;

        completionPlayed = true;
        sweepX = -getWidth();

        sweepPaint.setColor(allZero ? SWEEP_COLOR_INITIAL : SWEEP_COLOR_FINAL);

        sweepAnimator = ValueAnimator.ofFloat(-getWidth(), getWidth());

        sweepAnimator.setDuration(SWEEP_DURATION);
        sweepAnimator.setInterpolator(new FastOutSlowInInterpolator());

        if (allZero) {
            sweepAnimator.setRepeatCount(ValueAnimator.INFINITE);
        }

        sweepAnimator.addUpdateListener(a -> {
            sweepX = (float) a.getAnimatedValue();
            invalidate();
        });

        sweepAnimator.start();
    }

    /* ================= Draw ================= */

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rect.set(0, 0, w, h);
        clipPath.reset();
        clipPath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW);
    }

    @Override
    protected void onDraw(@NonNull Canvas c) {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;

        int save = c.save();
        c.clipPath(clipPath);

        c.drawRect(0, 0, w, h, bgPaint);

        float x = 0f;
        for (int i = 0; i < animated.length; i++) {
            float fw = animated[i] * w;
            if (fw <= 0f) continue;
            paint.setColor(i < colors.length ? colors[i] : 0);
            c.drawRect(x, 0, x + fw, h, paint);
            x += fw;
            if (x >= w) break;
        }

        if (!Float.isNaN(sweepX)) {
            float sweepWidth = w * 0.45f;
            c.drawRect(sweepX, 0, sweepX + sweepWidth, h, sweepPaint);
        }

        c.restoreToCount(save);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        syncBackgroundPaint();
    }

    /* ================= Reset & helpers ================= */

    private void resetTransientState() {
        if (animator != null) animator.cancel();
        if (sweepAnimator != null) sweepAnimator.cancel();
        animating = false;
        completionPlayed = false;
        sweepX = Float.NaN;
    }

    private void syncBackgroundPaint() {
        if (getBackground() instanceof android.graphics.drawable.ColorDrawable) {
            bgPaint.setColor(((android.graphics.drawable.ColorDrawable) getBackground()).getColor());
        }
    }

    private void ensureCapacity(float[] f) {
        int n = f != null ? f.length : 0;
        if (n == target.length) return;
        target = new float[n];
        animated = new float[n];
        start = new float[n];
    }

    private static float lerp(float s, float e, float t) {
        return s + (e - s) * t;
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }
}
