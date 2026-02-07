package com.github.jaykkumar01.vaultspace.views.creative.image;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

public final class VaultImageView extends AppCompatImageView {

    private static final float MAX_SCALE_STEP = 1.08f;
    private static final float MIN_SCALE_STEP = 0.92f;
    private static final float MAX_SCALE = 4f;
    private static final long DOUBLE_TAP_ANIM_MS = 200;
    private static final long RESET_ANIM_MS = 140;

    /* ---------------- state ---------------- */

    private Bitmap bitmap;
    private float baseScale, currentScale;
    private boolean isScaling;

    private final Matrix baseMatrix = new Matrix();
    private final Matrix drawMatrix = new Matrix();
    private final RectF imageRect = new RectF();
    private final RectF viewRect = new RectF();

    /* ---------------- rendering ---------------- */

    private boolean renderScheduled;

    /* ---------------- gestures / animation ---------------- */

    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;
    private ValueAnimator scaleAnimator;

    public VaultImageView(Context c, @Nullable AttributeSet a){
        super(c, a);
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(c, new ScaleListener());
        gestureDetector = new GestureDetector(c, new GestureListener());
    }

    /* ---------------- public API ---------------- */

    public void setImageBitmapSafe(Bitmap bmp){
        bitmap = bmp;
        super.setImageBitmap(bmp);
        post(this::computeBaseMatrix);
    }

    public void reset(){
        runScaleAnimation(currentScale, baseScale, getWidth() * .5f, getHeight() * .5f, RESET_ANIM_MS);
    }

    /* ---------------- base matrix ---------------- */

    private void computeBaseMatrix(){
        if(bitmap == null) return;

        float vw = getWidth(), vh = getHeight();
        if(vw == 0 || vh == 0) return;

        float bw = bitmap.getWidth(), bh = bitmap.getHeight();
        baseScale = Math.min(vw / bw, vh / bh);

        float dx = (vw - bw * baseScale) * .5f;
        float dy = (vh - bh * baseScale) * .5f;

        baseMatrix.reset();
        baseMatrix.postScale(baseScale, baseScale);
        baseMatrix.postTranslate(dx, dy);

        drawMatrix.set(baseMatrix);
        currentScale = baseScale;

        cancelAnimation();
        renderScheduled = false;
        renderNow();
    }

    @Override
    protected void onSizeChanged(int w,int h,int ow,int oh){
        super.onSizeChanged(w, h, ow, oh);
        computeBaseMatrix();
    }

    /* ---------------- touch ---------------- */

    @Override
    public boolean onTouchEvent(MotionEvent e){
        boolean r1 = scaleDetector.onTouchEvent(e);
        boolean r2 = gestureDetector.onTouchEvent(e);

        if(e.getAction() == MotionEvent.ACTION_UP)
            performClick();

        return r1 || r2 || super.onTouchEvent(e);
    }

    @Override
    public boolean performClick(){
        super.performClick();
        return true;
    }

    /* ---------------- render pipeline ---------------- */

    private void requestRender(){
        if(renderScheduled) return;

        renderScheduled = true;
        postOnAnimation(() -> {
            renderScheduled = false;
            renderNow();
        });
    }

    private void renderNow(){
        setImageMatrix(drawMatrix);
    }

    /* ---------------- math helpers (NO rendering) ---------------- */

    private void translateBy(float dx,float dy){
        drawMatrix.postTranslate(dx, dy);
        clamp();
    }

    private void scaleBy(float factor,float px,float py){
        float target = currentScale * factor;
        if(target < baseScale) factor = baseScale / currentScale;
        else if(target > MAX_SCALE) factor = MAX_SCALE / currentScale;

        drawMatrix.postTranslate(-px, -py);
        drawMatrix.postScale(factor, factor);
        drawMatrix.postTranslate(px, py);

        currentScale *= factor;
        clamp();
    }

    private void clamp(){
        if(bitmap == null) return;

        imageRect.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
        drawMatrix.mapRect(imageRect);

        viewRect.set(0, 0, getWidth(), getHeight());

        float dx = 0, dy = 0;

        if(imageRect.width() <= viewRect.width())
            dx = viewRect.centerX() - imageRect.centerX();
        else if(imageRect.left > 0) dx = -imageRect.left;
        else if(imageRect.right < viewRect.right) dx = viewRect.right - imageRect.right;

        if(imageRect.height() <= viewRect.height())
            dy = viewRect.centerY() - imageRect.centerY();
        else if(imageRect.top > 0) dy = -imageRect.top;
        else if(imageRect.bottom < viewRect.bottom) dy = viewRect.bottom - imageRect.bottom;

        drawMatrix.postTranslate(dx, dy);
    }

    /* ---------------- animation ---------------- */

    private void runScaleAnimation(float start,float end,float px,float py,long duration){
        cancelAnimation();

        final float[] last = { start };

        scaleAnimator = ValueAnimator.ofFloat(start, end);
        scaleAnimator.setDuration(duration);
        scaleAnimator.setInterpolator(new DecelerateInterpolator());
        scaleAnimator.addUpdateListener(a -> {
            float value = (float) a.getAnimatedValue();
            float factor = value / last[0];
            last[0] = value;

            scaleBy(factor, px, py);
            requestRender();
        });
        scaleAnimator.start();
    }

    private void cancelAnimation(){
        if(scaleAnimator != null){
            scaleAnimator.cancel();
            scaleAnimator = null;
        }
    }

    /* ---------------- gesture listeners ---------------- */

    private final class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScaleBegin(@NonNull ScaleGestureDetector d){
            isScaling = true;
            cancelAnimation();
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector d){
            float raw = d.getScaleFactor();
            float factor = raw > 1f
                    ? Math.min(raw, MAX_SCALE_STEP)
                    : Math.max(raw, MIN_SCALE_STEP);

            scaleBy(factor, d.getFocusX(), d.getFocusY());
            requestRender();
            return true;
        }

        @Override
        public void onScaleEnd(@NonNull ScaleGestureDetector d){
            isScaling = false;
        }
    }

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDoubleTap(@NonNull MotionEvent e){
            float target = currentScale > baseScale ? baseScale : baseScale * 2f;
            float px = currentScale > baseScale ? getWidth() * .5f : e.getX();
            float py = currentScale > baseScale ? getHeight() * .5f : e.getY();
            long dur = currentScale > baseScale ? RESET_ANIM_MS : DOUBLE_TAP_ANIM_MS;

            runScaleAnimation(currentScale, target, px, py, dur);
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1,@NonNull MotionEvent e2,float dx,float dy){
            if(isScaling || currentScale <= baseScale) return false;

            translateBy(-dx, -dy);
            requestRender();
            return true;
        }
    }
}
