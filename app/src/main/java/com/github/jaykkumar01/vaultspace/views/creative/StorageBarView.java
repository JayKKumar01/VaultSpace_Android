package com.github.jaykkumar01.vaultspace.views.creative;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.jaykkumar01.vaultspace.R;

import java.util.Locale;

public final class StorageBarView extends View {

    /* ---------- config ---------- */

    private static final long SWEEP_DURATION_MS = 1400L;
    private static final int UPS = 45;
    private static final long FRAME_MS = 1000L / UPS;
    private static final String LABEL = "Your Space · ";
    private static final String LOADING = "–";
    private static final int DECIMALS = 1;

    /* ---------- paints ---------- */

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sweepPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF rect = new RectF();
    private final Matrix sweepMatrix = new Matrix();

    /* ---------- state ---------- */

    private boolean hasUsage;
    private float used, total, fraction;
    private String unit = "GB";

    private float contentWidth;
    private float barHeight, cornerRadius, textSpacing;

    /* ---------- sweep ---------- */

    private LinearGradient sweepGradient;
    private float sweepProgress;
    private float sweepX;
    private long lastMs;
    private boolean running;

    /* ---------- ctor ---------- */

    public StorageBarView(Context c) {
        super(c);
        init(c);
    }

    public StorageBarView(Context c, @Nullable AttributeSet a) {
        super(c, a);
        init(c);
    }

    public StorageBarView(Context c, @Nullable AttributeSet a, int s) {
        super(c, a, s);
        init(c);
    }

    private void init(Context c) {
        bgPaint.setColor(c.getColor(R.color.vs_toggle_off));
        fillPaint.setColor(c.getColor(R.color.vs_accent_primary));
        textPaint.setColor(c.getColor(R.color.vs_text_content));
        textPaint.setTextSize(sp(11));
        textPaint.setFakeBoldText(true);

        barHeight = dp(4);
        cornerRadius = dp(2);
        textSpacing = dp(4);
        contentWidth = textPaint.measureText(LABEL + LOADING);
    }

    /* ---------- layout ---------- */

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        if (w <= 0) return;
        sweepGradient = new LinearGradient(0, 0, w, 0,
                new int[]{0x00FFFFFF, 0x22FFFFFF, 0x88FFFFFF, 0xCCFFFFFF, 0x88FFFFFF, 0x22FFFFFF, 0x00FFFFFF},
                new float[]{0f, .30f, .40f, .50f, .60f, .70f, 1f},
                Shader.TileMode.CLAMP);
        sweepPaint.setShader(sweepGradient);
    }

    @Override
    protected void onMeasure(int ws, int hs) {
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float th = fm.descent - fm.ascent;
        setMeasuredDimension((int) Math.ceil(contentWidth), (int) Math.ceil(th + textSpacing + barHeight));
    }

    /* ---------- draw ---------- */

    @Override
    protected void onDraw(@NonNull Canvas c) {
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textY = -fm.ascent;
        c.drawText(text(), 0, textY, textPaint);

        float top = textY + fm.descent + textSpacing;
        rect.set(0, top, contentWidth, top + barHeight);
        c.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint);

        if (hasUsage) {
            if (fraction > 0f) {
                rect.right = contentWidth * fraction;
                c.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint);
            }
            return;
        }

        sweepMatrix.setTranslate(sweepX, 0);
        sweepGradient.setLocalMatrix(sweepMatrix);
        c.drawRoundRect(rect, cornerRadius, cornerRadius, sweepPaint);
    }

    /* ---------- public ---------- */

    public void setUsage(float used, float total, @NonNull String unit) {

        boolean firstUsage = !hasUsage;

        this.used = used;
        this.total = total;
        this.unit = unit;

        float newFraction = total <= 0f ? 0f : Math.min(1f, used / total);

        // Only stop sweep on FIRST real usage
        if (firstUsage) {
            hasUsage = true;
            stopSweep();
        }

        // Update fraction every time
        fraction = newFraction;

        // Re-measure text if value changed
        contentWidth = textPaint.measureText(text());

        requestLayout();
        invalidate();
    }


    /* ---------- lifecycle ---------- */

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!hasUsage) startSweep();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopSweep();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View v, int vis) {
        super.onVisibilityChanged(v, vis);
        if (vis == VISIBLE && !hasUsage) startSweep();
        else stopSweep();
    }

    /* ---------- sweep ticker ---------- */

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            long now = SystemClock.uptimeMillis();
            float dt = (now - lastMs) / (float) SWEEP_DURATION_MS;
            lastMs = now;

            sweepProgress += dt;
            if (sweepProgress > 1f) sweepProgress -= 1f;

            sweepX = lerp(-contentWidth, contentWidth, sweepProgress);
            invalidate();
            postDelayed(this, FRAME_MS);
        }
    };

    private void startSweep() {
        if (running) return;
        running = true;
        sweepProgress = 0f;
        lastMs = SystemClock.uptimeMillis();
        removeCallbacks(ticker);
        post(ticker);
    }

    private void stopSweep() {
        running = false;
        removeCallbacks(ticker);
    }

    /* ---------- helpers ---------- */

    private String text() {
        if (!hasUsage) return LABEL + LOADING;
        return LABEL + fmt(used) + " / " + fmt(total) + " " + unit;
    }

    private String fmt(float v) {
        return String.format(Locale.US, "%." + DECIMALS + "f", v);
    }

    private static float lerp(float s, float e, float t) {
        return s + (e - s) * t;
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private float sp(float v) {
        return v * getResources().getDisplayMetrics().scaledDensity;
    }
}
