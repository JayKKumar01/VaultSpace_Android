package com.github.jaykkumar01.vaultspace.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.jaykkumar01.vaultspace.R;

import java.util.Locale;

public class StorageBarView extends View {

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    /* ================= State ================= */

    private boolean isLoading = true;

    private float usageFraction = 0f;
    private float usedValue;
    private float totalValue;
    private String unitText = "GB";

    private static final String LABEL = "Your Space · ";
    private static final String LOADING_VALUE = "–";

    /* ================= Precision ================= */

    private static final int DECIMAL_PRECISION = 1;

    /* ================= Animation tuning ================= */

    private static final int LOADING_FPS = 24;
    private static final long FRAME_DELAY_MS = 1000L / LOADING_FPS;
    private static final float SPEED_PER_MS = 0.0006f;

    private static final float LOADING_BAR_WIDTH_FRACTION = 0.40f;
    private static final int LOADING_BAR_ALPHA = 160;

    private float loadingProgress = 0f;
    private long lastFrameTime = 0L;

    /* ================= Dimensions ================= */

    private float barHeight;
    private float cornerRadius;
    private float textSpacing;

    private float contentWidth;
    private float loadingTextWidth;

    /* ================= Animation runnable ================= */

    private final Runnable loadingTicker = new Runnable() {
        @Override
        public void run() {
            if (!isLoading || !isAttachedToWindow()) return;

            long now = System.currentTimeMillis();

            if (lastFrameTime != 0L) {
                long delta = now - lastFrameTime;
                loadingProgress += delta * SPEED_PER_MS;
                if (loadingProgress > 1f) loadingProgress = 0f;
            }

            lastFrameTime = now;
            invalidate();
            postOnAnimationDelayed(this, FRAME_DELAY_MS);
        }
    };

    /* ================= Constructors ================= */

    public StorageBarView(Context context) {
        super(context);
        init(context, null);
    }

    public StorageBarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public StorageBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        backgroundPaint.setColor(context.getColor(R.color.vs_toggle_off));
        fillPaint.setColor(context.getColor(R.color.vs_accent_primary));

        textPaint.setColor(context.getColor(R.color.vs_text_content));
        textPaint.setTextSize(spToPx(11));
        textPaint.setFakeBoldText(true);

        barHeight = dpToPx(4);
        cornerRadius = dpToPx(2);
        textSpacing = dpToPx(4);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.StorageBarView);
            if (a.hasValue(R.styleable.StorageBarView_usedText)) {
                isLoading = false;
                usedValue = a.getFloat(R.styleable.StorageBarView_usedText, 0f);
                totalValue = a.getFloat(R.styleable.StorageBarView_totalText, 0f);
                unitText = getOrDefault(
                        a.getString(R.styleable.StorageBarView_unitText),
                        unitText
                );
                usageFraction = a.getFloat(
                        R.styleable.StorageBarView_usageFraction,
                        0f
                );
            }
            a.recycle();
        }

        loadingTextWidth = textPaint.measureText(LABEL + LOADING_VALUE);
        recalculateContentWidth();

        if (isLoading) startLoadingAnimation();
    }

    /* ================= Layout ================= */

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textHeight = fm.descent - fm.ascent;

        setMeasuredDimension(
                (int) Math.ceil(contentWidth),
                (int) Math.ceil(textHeight + textSpacing + barHeight)
        );
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textY = -fm.ascent;

        canvas.drawText(buildText(), 0, textY, textPaint);

        float barTop = textY + fm.descent + textSpacing;
        float barBottom = barTop + barHeight;

        rect.set(0, barTop, contentWidth, barBottom);
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, backgroundPaint);

        if (isLoading) {
            float segmentWidth = contentWidth * LOADING_BAR_WIDTH_FRACTION;
            float travelWidth = contentWidth + segmentWidth;

            float startX = (loadingProgress * travelWidth) - segmentWidth;
            float endX = startX + segmentWidth;

            if (endX > 0 && startX < contentWidth) {
                fillPaint.setAlpha(LOADING_BAR_ALPHA);
                rect.set(
                        Math.max(0, startX),
                        barTop,
                        Math.min(contentWidth, endX),
                        barBottom
                );
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint);
                fillPaint.setAlpha(255);
            }
        } else if (usageFraction > 0f) {
            rect.set(0, barTop, contentWidth * usageFraction, barBottom);
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint);
        }
    }

    /* ================= Public API ================= */

    // 🔒 Backward compatible
    public void setUsage(long used, long total, @NonNull String unit) {
        setUsage((float) used, (float) total, unit);
    }

    // ✅ Decimal-aware API
    public void setUsage(float used, float total, @NonNull String unit) {
        stopLoadingAnimation();

        isLoading = false;
        usedValue = used;
        totalValue = total;
        unitText = unit;

        usageFraction = total <= 0f
                ? 0f
                : Math.min(1f, used / total);

        recalculateContentWidth();
        requestLayout();
        invalidate();
    }

    public void setLoading(boolean loading) {
        if (loading == isLoading) return;

        isLoading = loading;

        if (loading) startLoadingAnimation();
        else stopLoadingAnimation();

        recalculateContentWidth();
        requestLayout();
        invalidate();
    }

    /* ================= Animation lifecycle ================= */

    private void startLoadingAnimation() {
        removeCallbacks(loadingTicker);
        loadingProgress = 0f;
        lastFrameTime = 0L;
        postOnAnimationDelayed(loadingTicker, FRAME_DELAY_MS);
    }

    private void stopLoadingAnimation() {
        removeCallbacks(loadingTicker);
        loadingProgress = 0f;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopLoadingAnimation();
    }

    /* ================= Helpers ================= */

    private String buildText() {
        if (isLoading) {
            return LABEL + LOADING_VALUE;
        }

        return LABEL
                + format(usedValue)
                + " / "
                + format(totalValue)
                + " "
                + unitText;
    }

    private String format(float value) {
        return String.format(
                Locale.US,
                "%." + DECIMAL_PRECISION + "f",
                value
        );
    }

    private void recalculateContentWidth() {
        contentWidth = isLoading
                ? loadingTextWidth
                : textPaint.measureText(buildText());
    }

    private String getOrDefault(@Nullable String value, String fallback) {
        return value != null ? value : fallback;
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }
}
