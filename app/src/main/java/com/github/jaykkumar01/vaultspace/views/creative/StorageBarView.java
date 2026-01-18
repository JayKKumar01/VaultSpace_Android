package com.github.jaykkumar01.vaultspace.views.creative;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.jaykkumar01.vaultspace.R;

import java.util.Locale;

public class StorageBarView extends View {

    /* ================= Paints & geometry ================= */

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sweepPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    /* ================= State ================= */

    private boolean hasUsage = false;

    private float usageFraction;
    private float usedValue;
    private float totalValue;
    private String unitText = "GB";

    private static final String LABEL = "Your Space · ";
    private static final String LOADING_VALUE = "–";
    private static final int DECIMAL_PRECISION = 1;

    /* ================= Sweep ================= */

    private static final long SWEEP_DURATION_MS = 1400L;
    private static final int UPS = 45;

    private UpsTickerAnimator sweepAnimator;

    private LinearGradient sweepGradient;
    private final Matrix sweepMatrix = new Matrix();
    private float sweepTranslateX;

    /* ================= Dimensions ================= */

    private float barHeight;
    private float cornerRadius;
    private float textSpacing;
    private float contentWidth;

    /* ================= Constructors ================= */

    public StorageBarView(Context context) {
        super(context);
        init(context);
    }

    public StorageBarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public StorageBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        backgroundPaint.setColor(context.getColor(R.color.vs_toggle_off));
        fillPaint.setColor(context.getColor(R.color.vs_accent_primary));

        textPaint.setColor(context.getColor(R.color.vs_text_content));
        textPaint.setTextSize(spToPx(11));
        textPaint.setFakeBoldText(true);

        barHeight = dpToPx(4);
        cornerRadius = dpToPx(2);
        textSpacing = dpToPx(4);

        contentWidth = textPaint.measureText(LABEL + LOADING_VALUE);
    }

    /* ================= Layout & shader ================= */

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0) return;

        sweepGradient = new LinearGradient(0f, 0f, w, 0f,
                new int[]{0x00FFFFFF, 0x22FFFFFF, 0x88FFFFFF, 0xCCFFFFFF, 0x88FFFFFF, 0x22FFFFFF, 0x00FFFFFF},
                new float[]{0f, 0.30f, 0.40f, 0.50f, 0.60f, 0.70f, 1f},
                Shader.TileMode.CLAMP);


        sweepPaint.setShader(sweepGradient);
        ensureAnimator();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textHeight = fm.descent - fm.ascent;

        setMeasuredDimension(
                (int) Math.ceil(contentWidth),
                (int) Math.ceil(textHeight + textSpacing + barHeight)
        );
    }

    /* ================= Draw ================= */

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textY = -fm.ascent;

        canvas.drawText(buildText(), 0f, textY, textPaint);

        float barTop = textY + fm.descent + textSpacing;
        float barBottom = barTop + barHeight;

        rect.set(0f, barTop, contentWidth, barBottom);
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, backgroundPaint);

        if (hasUsage) {
            if (usageFraction > 0f) {
                rect.right = contentWidth * usageFraction;
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint);
            }
            return;
        }

        sweepMatrix.setTranslate(sweepTranslateX, 0f);
        sweepGradient.setLocalMatrix(sweepMatrix);
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, sweepPaint);
    }

    /* ================= Public API ================= */

    public void setUsage(float used, float total, @NonNull String unit) {
        if (hasUsage) return;

        hasUsage = true;
        if (sweepAnimator != null) sweepAnimator.stop();

        usedValue = used;
        totalValue = total;
        unitText = unit;

        usageFraction = total <= 0f ? 0f : Math.min(1f, used / total);

        contentWidth = textPaint.measureText(buildText());
        requestLayout();
        invalidate();
    }

    /* ================= Lifecycle ================= */

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (sweepAnimator != null) sweepAnimator.onAttached();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (sweepAnimator != null) sweepAnimator.onDetached();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (sweepAnimator != null) {
            sweepAnimator.onVisibilityChanged(visibility == VISIBLE);
        }
    }

    /* ================= Animator ================= */

    private void ensureAnimator() {
        if (sweepAnimator != null || hasUsage) return;

        sweepAnimator = new UpsTickerAnimator(
                this,
                SWEEP_DURATION_MS,
                UPS,
                UPS,
                progress -> {
                    sweepTranslateX = lerp(-contentWidth, contentWidth, progress);
                    invalidate();
                }
        );

        // 🔥 Critical lifecycle sync (FIX)
        if (isAttachedToWindow()) {
            sweepAnimator.onAttached();
            sweepAnimator.onVisibilityChanged(getVisibility() == VISIBLE);
        }
    }

    /* ================= Helpers ================= */

    private String buildText() {
        if (!hasUsage) return LABEL + LOADING_VALUE;
        return LABEL + format(usedValue) + " / " + format(totalValue) + " " + unitText;
    }

    private String format(float value) {
        return String.format(Locale.US, "%." + DECIMAL_PRECISION + "f", value);
    }

    private static float lerp(float s, float e, float t) {
        return s + (e - s) * t;
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }
}
