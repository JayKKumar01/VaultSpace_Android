package com.github.jaykkumar01.vaultspace.views.anim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SplashBorderView extends View {

    /* ==========================================================
     * Configuration
     * ========================================================== */

    private static final long LOOP_DURATION_MS     = 4000L;
    private static final long FINALIZE_DURATION_MS = 2000L;

    private static final float STROKE_SIZE = 6f;
    private static final float CORNER_RADIUS  = 48f;
    private static final float SEGMENT_RATIO  = 0.12f;

    /* ==========================================================
     * State
     * ========================================================== */

    private enum State {
        LOOP,
        FINALIZE,
        IDLE
    }

    private State state = State.LOOP;

    /* ==========================================================
     * Drawing Tools
     * ========================================================== */

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path fullPath = new Path();
    private final Path segmentPath = new Path();
    private final RectF rect = new RectF();
    private final PathMeasure pathMeasure = new PathMeasure();

    private float pathLength = 0f;

    /* ==========================================================
     * Timing
     * ========================================================== */

    private long loopStartTime;
    private long finalizeStartTime;

    private Runnable finishCallback;

    /* ==========================================================
     * Constructor
     * ========================================================== */

    public SplashBorderView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(STROKE_SIZE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(0xFF7BE495); // default accent
    }

    public void setAccentColor(int color) {
        paint.setColor(color);
        invalidate();
    }

    /* ==========================================================
     * Lifecycle Awareness
     * ========================================================== */

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startIfVisible();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopFrames();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        handleVisibility();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        handleVisibility();
    }

    private void handleVisibility() {
        if (isNotVisible()) {
            stopFrames();
            return;
        }

        if (state == State.FINALIZE) {
            // restart final animation cleanly
            finalizeStartTime = SystemClock.uptimeMillis();
        }

        startIfVisible();
    }

    private boolean isNotVisible() {
        return !isAttachedToWindow()
                || getVisibility() != VISIBLE
                || getWindowVisibility() != VISIBLE;
    }

    /* ==========================================================
     * Frame Loop
     * ========================================================== */

    private void startIfVisible() {
        if (isNotVisible() || state == State.IDLE) return;

        if (loopStartTime == 0L) {
            loopStartTime = SystemClock.uptimeMillis();
        }

        postOnAnimation(frameRunnable);
    }

    private void stopFrames() {
        removeCallbacks(frameRunnable);
    }

    private final Runnable frameRunnable = new Runnable() {
        @Override
        public void run() {
            if (isNotVisible() || state == State.IDLE) return;

            invalidate();
            postOnAnimation(this);
        }
    };

    /* ==========================================================
     * Public Finalize API
     * ========================================================== */

    public void clearAnimationNow(@Nullable Runnable onFinished) {

        if (state != State.LOOP) return;

        state = State.FINALIZE;
        finalizeStartTime = SystemClock.uptimeMillis();
        finishCallback = onFinished;
    }

    /* ==========================================================
     * Layout Handling
     * ========================================================== */

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {

        rect.set(
                STROKE_SIZE,
                STROKE_SIZE,
                w - STROKE_SIZE,
                h - STROKE_SIZE
        );

        fullPath.reset();
        fullPath.addRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, Path.Direction.CW);

        pathMeasure.setPath(fullPath, true);
        pathLength = pathMeasure.getLength();
    }

    /* ==========================================================
     * Drawing
     * ========================================================== */

    @Override
    protected void onDraw(@NonNull Canvas canvas) {

        if (pathLength == 0f) return;

        long now = SystemClock.uptimeMillis();

        switch (state) {

            case LOOP:
                drawLoop(canvas, now);
                break;

            case FINALIZE:
                drawFinalize(canvas, now);
                break;

            case IDLE:
            default:
                break;
        }
    }

    /* ==========================================================
     * LOOP State
     * ========================================================== */

    private void drawLoop(Canvas canvas, long now) {

        float progress =
                ((now - loopStartTime) % LOOP_DURATION_MS)
                        / (float) LOOP_DURATION_MS;

        drawSegment(canvas, progress, pathLength * SEGMENT_RATIO);
    }

    /* ==========================================================
     * FINALIZE State
     * ========================================================== */

    private void drawFinalize(Canvas canvas, long now) {

        float elapsed =
                (now - finalizeStartTime) / (float) FINALIZE_DURATION_MS;

        float t = Math.min(1f, elapsed);

        // cubic ease-out
        float eased = 1f - (float) Math.pow(1f - t, 3);

        float progress =
                ((now - loopStartTime) % LOOP_DURATION_MS)
                        / (float) LOOP_DURATION_MS;

        float segmentLength =
                pathLength * (SEGMENT_RATIO + (1f - SEGMENT_RATIO) * eased);

        drawSegment(canvas, progress, segmentLength);

        if (t >= 1f) {
            state = State.IDLE;
            stopFrames();

            if (finishCallback != null) {
                finishCallback.run();
                finishCallback = null;
            }
        }
    }

    /* ==========================================================
     * Segment Drawing Helper
     * ========================================================== */

    private void drawSegment(Canvas canvas, float progress, float length) {

        float start = progress * pathLength;
        float end = start + length;

        segmentPath.reset();

        if (end <= pathLength) {
            pathMeasure.getSegment(start, end, segmentPath, true);
        } else {
            pathMeasure.getSegment(start, pathLength, segmentPath, true);
            pathMeasure.getSegment(0, end - pathLength, segmentPath, true);
        }

        canvas.drawPath(segmentPath, paint);
    }
}
