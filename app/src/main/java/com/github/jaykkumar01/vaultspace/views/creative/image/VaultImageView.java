package com.github.jaykkumar01.vaultspace.views.creative.image;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

public final class VaultImageView extends AppCompatImageView {

    private Bitmap bitmap;
    private final Matrix baseMatrix = new Matrix();
    private final Matrix drawMatrix = new Matrix();
    private final RectF imageRect = new RectF();
    private final RectF viewRect = new RectF();

    private float baseScale;
    private float currentScale;
    private boolean isScaling;

    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;

    public VaultImageView(Context c, @Nullable AttributeSet a) {
        super(c, a);
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(c, new ScaleListener());
        gestureDetector = new GestureDetector(c, new GestureListener());
    }

    /* ---------------- public API ---------------- */

    public void setImageBitmapSafe(Bitmap bmp) {
        bitmap = bmp;
        super.setImageBitmap(bmp);
        post(this::computeBaseMatrix);
    }

    public void reset() {
        drawMatrix.set(baseMatrix);
        currentScale = baseScale;
        setImageMatrix(drawMatrix);
        invalidate();
    }

    /* ---------------- core logic ---------------- */

    private void computeBaseMatrix() {
        if (bitmap == null) return;
        float vw = getWidth(), vh = getHeight(), bw = bitmap.getWidth(), bh = bitmap.getHeight();
        if (vw == 0 || vh == 0) return;

        baseMatrix.reset();
        baseScale = Math.min(vw / bw, vh / bh);
        float dx = (vw - bw * baseScale) / 2f, dy = (vh - bh * baseScale) / 2f;
        baseMatrix.postScale(baseScale, baseScale);
        baseMatrix.postTranslate(dx, dy);

        drawMatrix.set(baseMatrix);
        currentScale = baseScale;
        setImageMatrix(drawMatrix);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        computeBaseMatrix();
    }

    /* ---------------- touch ---------------- */

    @Override
    public boolean onTouchEvent(MotionEvent e){
        boolean r1=scaleDetector.onTouchEvent(e);
        boolean r2=gestureDetector.onTouchEvent(e);

        if(e.getAction()==MotionEvent.ACTION_UP){
            performClick();
        }

        return r1||r2||super.onTouchEvent(e);
    }


    @Override
    public boolean performClick(){
        super.performClick();
        return true;
    }


    /* ---------------- helpers ---------------- */

    private void applyScale(float factor, float px, float py) {
        float target = currentScale * factor;
        float maxScale = 4f;
        if (target < baseScale) factor = baseScale / currentScale;
        else if (target > maxScale) factor = maxScale / currentScale;

        drawMatrix.postTranslate(-px, -py);
        drawMatrix.postScale(factor, factor);
        drawMatrix.postTranslate(px, py);
        currentScale *= factor;
        clamp();
        setImageMatrix(drawMatrix);
    }

    private void applyTranslation(float dx, float dy) {
        drawMatrix.postTranslate(dx, dy);
        clamp();
        setImageMatrix(drawMatrix);
    }

    private void clamp() {
        if (bitmap == null) return;
        imageRect.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
        drawMatrix.mapRect(imageRect);

        viewRect.set(0, 0, getWidth(), getHeight());

        float dx = 0, dy = 0;

        if (imageRect.width() <= viewRect.width())
            dx = viewRect.centerX() - imageRect.centerX();
        else {
            if (imageRect.left > 0) dx = -imageRect.left;
            else if (imageRect.right < viewRect.right) dx = viewRect.right - imageRect.right;
        }

        if (imageRect.height() <= viewRect.height())
            dy = viewRect.centerY() - imageRect.centerY();
        else {
            if (imageRect.top > 0) dy = -imageRect.top;
            else if (imageRect.bottom < viewRect.bottom) dy = viewRect.bottom - imageRect.bottom;
        }

        drawMatrix.postTranslate(dx, dy);
    }

    /* ---------------- gesture listeners ---------------- */

    private final class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(@NonNull ScaleGestureDetector d) {
            isScaling = true;
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector d) {
            applyScale(d.getScaleFactor(), d.getFocusX(), d.getFocusY());
            return true;
        }

        @Override
        public void onScaleEnd(@NonNull ScaleGestureDetector d) {
            isScaling = false;
        }
    }

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDoubleTap(@NonNull MotionEvent e) {
            if (currentScale > baseScale) reset();
            else applyScale(2f, e.getX(), e.getY());
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2, float dx, float dy) {
            if (isScaling || currentScale <= baseScale) return false;
            applyTranslation(-dx, -dy);
            return true;
        }
    }
}
