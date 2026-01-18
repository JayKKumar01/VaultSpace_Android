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
    private static final long PULSE_DURATION = 260L;

    private float[] target = new float[0];
    private float[] animated = new float[0];
    private float[] start = new float[0];
    private int[] colors = new int[0];

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path clipPath = new Path();
    private final RectF rect = new RectF();
    private float cornerRadius;

    private long duration = DEFAULT_DURATION;
    private TimeInterpolator interpolator = new FastOutSlowInInterpolator();

    private long animStart;
    private boolean animating;
    private ValueAnimator animator;

    private float pulseScale = 1f;
    private boolean pulsePlayed;

    public MultiSegmentProgressBar(Context c) { super(c); init(); }
    public MultiSegmentProgressBar(Context c, @Nullable AttributeSet a) { super(c, a); init(); }

    private void init() {
        bgPaint.setStyle(Paint.Style.FILL);
        cornerRadius = getResources().getDisplayMetrics().density * 2f;
    }

    /* ================= Public API ================= */

    public void setColors(int[] colors) {
        this.colors = colors != null ? colors.clone() : new int[0];
        invalidate();
    }

    public void setFractions(float[] fractions) {
        pulsePlayed = false;
        pulseScale = 1f;
        ensureCapacity(fractions);
        for (int i = 0; i < fractions.length; i++) {
            target[i] = clamp01(fractions[i]);
        }
        startAnimationIfNeeded();
    }

    public void setFractionsImmediate(float[] fractions) {
        pulsePlayed = false;
        pulseScale = 1f;
        ensureCapacity(fractions);
        for (int i = 0; i < fractions.length; i++) {
            float v = clamp01(fractions[i]);
            target[i] = v;
            animated[i] = v;
            start[i] = v;
        }
        stopAnimator();
        invalidate();
    }

    public void setAnimationDuration(long millis) {
        duration = Math.max(0, millis);
    }

    public void setInterpolator(@Nullable TimeInterpolator i) {
        interpolator = i != null ? i : new FastOutSlowInInterpolator();
    }

    /* ================= Animation ================= */

    private void startAnimationIfNeeded() {
        if (animating) return;
        boolean diff = false;
        for (int i = 0; i < target.length; i++) {
            if (animated[i] != target[i]) { diff = true; break; }
        }
        if (!diff) return;

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
            maybeStartPulse();
        }
    }

    private void maybeStartPulse() {
        if (pulsePlayed) return;
        float sum = 0f;
        for (float v : target) sum += v;
        if (sum < 0.999f) return;

        pulsePlayed = true;
        ValueAnimator pulseAnimator = ValueAnimator.ofFloat(1f, 1.08f, 1f);
        pulseAnimator.setDuration(PULSE_DURATION);
        pulseAnimator.setInterpolator(new FastOutSlowInInterpolator());
        pulseAnimator.addUpdateListener(a -> {
            pulseScale = (float) a.getAnimatedValue();
            invalidate();
        });
        pulseAnimator.start();
    }

    private void stopAnimator() {
        if (animator != null) animator.cancel();
        animating = false;
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

        c.restoreToCount(save);

        if (pulseScale != 1f) {
            c.save();
            c.scale(pulseScale, pulseScale, w / 2f, h / 2f);
            c.drawRect(0, 0, w, h, bgPaint);
            c.restore();
        }
    }

    /* ================= Helpers ================= */

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
