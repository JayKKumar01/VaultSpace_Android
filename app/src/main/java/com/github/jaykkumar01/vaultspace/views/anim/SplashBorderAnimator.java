package com.github.jaykkumar01.vaultspace.views.anim;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SplashBorderAnimator {

    private static final long DURATION_MS = 4000L;

    private final BorderDrawable drawable;

    private final long startTime;
    private boolean running = true;

    public SplashBorderAnimator(View target, int accentColor) {

        drawable = new BorderDrawable(accentColor);
        target.setBackground(drawable);

        startTime = SystemClock.uptimeMillis();
        // next frame
        Runnable frameRunnable = new Runnable() {
            @Override
            public void run() {
                if (!running) return;

                long now = SystemClock.uptimeMillis();
                float progress = ((now - startTime) % DURATION_MS) / (float) DURATION_MS;

                drawable.setProgress(progress);
                target.invalidate();

                target.postOnAnimation(this); // next frame
            }
        };
        target.post(frameRunnable);
    }

    public void release() {
        running = false;
    }

    /* ==========================================================
     * Border Drawable
     * ========================================================== */

    private static class BorderDrawable extends Drawable {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path fullPath = new Path();
        private final Path segmentPath = new Path();
        private final RectF rect = new RectF();
        private final PathMeasure pathMeasure = new PathMeasure();

        private float progress = 0f;

        private static final float STROKE_WIDTH = 6f;
        private static final float STROKE_HEIGHT = 6f;
        private static final float CORNER_RADIUS = 48f;

        BorderDrawable(int accentColor) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(STROKE_WIDTH);
            paint.setColor(accentColor);
            paint.setStrokeCap(Paint.Cap.ROUND);
        }

        void setProgress(float p) {
            progress = p;
        }

        @Override
        public void draw(@NonNull Canvas canvas) {

            float w = getBounds().width();
            float h = getBounds().height();

            rect.set(
                    STROKE_WIDTH,
                    STROKE_HEIGHT,
                    w - STROKE_WIDTH,
                    h - STROKE_WIDTH
            );

            fullPath.reset();
            fullPath.addRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, Path.Direction.CW);

            pathMeasure.setPath(fullPath, true);

            float length = pathMeasure.getLength();
            float segmentLength = length * 0.12f;

            float start = progress * length;
            float end = start + segmentLength;

            segmentPath.reset();

            if (end <= length) {
                pathMeasure.getSegment(start, end, segmentPath, true);
            } else {
                pathMeasure.getSegment(start, length, segmentPath, true);
                pathMeasure.getSegment(0, end - length, segmentPath, true);
            }

            canvas.drawPath(segmentPath, paint);
        }

        @Override public void setAlpha(int alpha) {}
        @Override public void setColorFilter(@Nullable android.graphics.ColorFilter colorFilter) {}
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
    }
}
