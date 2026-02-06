package com.github.jaykkumar01.vaultspace.media.controller;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

public final class ZoomableImageView extends AppCompatImageView {

    private static final float MIN_SCALE = 1f;
    private static final float MAX_SCALE = 4f;
    private static final float DOUBLE_TAP_SCALE = 2.5f;

    private final Matrix matrix = new Matrix();
    private float currentScale = 1f;

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    public ZoomableImageView(Context c) { super(c); init(); }
    public ZoomableImageView(Context c, AttributeSet a) { super(c, a); init(); }
    public ZoomableImageView(Context c, AttributeSet a, int s) { super(c, a, s); init(); }

    private void init() {
        setScaleType(ScaleType.MATRIX);
        setImageMatrix(matrix);
        setClickable(true);

        scaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        gestureDetector = new GestureDetector(getContext(), new GestureListener());
    }

    public void resetZoom() {
        matrix.reset();
        currentScale = 1f;
        setImageMatrix(matrix);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        gestureDetector.onTouchEvent(e);
        scaleDetector.onTouchEvent(e);

        if (e.getActionMasked() == MotionEvent.ACTION_UP) performClick();
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private final class ScaleListener
            extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector d) {
            float factor = d.getScaleFactor();
            float target = currentScale * factor;

            if (target < MIN_SCALE) factor = MIN_SCALE / currentScale;
            if (target > MAX_SCALE) factor = MAX_SCALE / currentScale;

            matrix.postScale(factor, factor, d.getFocusX(), d.getFocusY());
            currentScale *= factor;
            setImageMatrix(matrix);
            return true;
        }
    }

    private final class GestureListener
            extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDoubleTap(@NonNull MotionEvent e) {
            if (currentScale > MIN_SCALE)
                animateScale(currentScale, MIN_SCALE,
                        getWidth() / 2f, getHeight() / 2f);
            else
                animateScale(currentScale, DOUBLE_TAP_SCALE,
                        e.getX(), e.getY());
            return true;
        }
    }

    private void animateScale(float from, float to, float px, float py) {
        ValueAnimator a = ValueAnimator.ofFloat(from, to);
        a.setDuration(200);
        a.addUpdateListener(v -> {
            float value = (float) v.getAnimatedValue();
            float factor = value / currentScale;
            matrix.postScale(factor, factor, px, py);
            currentScale = value;
            setImageMatrix(matrix);
        });
        a.start();
    }
}
