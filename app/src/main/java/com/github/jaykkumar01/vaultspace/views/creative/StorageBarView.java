package com.github.jaykkumar01.vaultspace.views.creative;

import android.content.Context;
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

    private boolean hasUsage = false;

    private float usageFraction = 0f;
    private float usedValue;
    private float totalValue;
    private String unitText = "GB";

    private static final String LABEL = "Your Space · ";
    private static final String LOADING_VALUE = "–";
    private static final int DECIMAL_PRECISION = 1;

    /* ================= Dimensions ================= */

    private float barHeight;
    private float cornerRadius;
    private float textSpacing;

    private float contentWidth;
    private float loadingTextWidth;

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

        loadingTextWidth = textPaint.measureText(LABEL + LOADING_VALUE);
        contentWidth = loadingTextWidth;
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
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textY = -fm.ascent;

        canvas.drawText(buildText(), 0, textY, textPaint);

        float barTop = textY + fm.descent + textSpacing;
        float barBottom = barTop + barHeight;

        rect.set(0, barTop, contentWidth, barBottom);
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, backgroundPaint);

        if (hasUsage && usageFraction > 0f) {
            rect.set(0, barTop, contentWidth * usageFraction, barBottom);
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint);
        }
    }

    /* ================= Public API ================= */

    public void setUsage(long used, long total, @NonNull String unit) {
        setUsage((float) used, (float) total, unit);
    }

    public void setUsage(float used, float total, @NonNull String unit) {
        hasUsage = true;

        usedValue = used;
        totalValue = total;
        unitText = unit;

        usageFraction = total <= 0f
                ? 0f
                : Math.min(1f, used / total);

        contentWidth = textPaint.measureText(buildText());
        requestLayout();
        invalidate();
    }

    /* ================= Helpers ================= */

    private String buildText() {
        if (!hasUsage) {
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

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }
}
