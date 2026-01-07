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

public class StorageBarView extends View {

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    // ===== Preview defaults =====
    private float usageFraction = 0.4f;
    private String usedText = "40";
    private String totalText = "100";
    private String unitText = "GB";

    private static final String LABEL = "Your Space · ";

    // ===== Dimensions =====
    private float barHeight;
    private float cornerRadius;
    private float textSpacing;

    // Cached width
    private float contentWidth;

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
        textPaint.setTextSize(spToPx());
        textPaint.setFakeBoldText(true);

        barHeight = dpToPx(4);
        cornerRadius = dpToPx(2);
        textSpacing = dpToPx(4);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.StorageBarView);

            usageFraction = a.getFloat(
                    R.styleable.StorageBarView_usageFraction,
                    usageFraction
            );

            usedText = getOrDefault(a.getString(R.styleable.StorageBarView_usedText), usedText);
            totalText = getOrDefault(a.getString(R.styleable.StorageBarView_totalText), totalText);
            unitText = getOrDefault(a.getString(R.styleable.StorageBarView_unitText), unitText);

            a.recycle();
        }

        recalculateContentWidth();
    }

    private void recalculateContentWidth() {
        contentWidth = textPaint.measureText(buildText());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textHeight = fm.descent - fm.ascent;

        int width = (int) Math.ceil(contentWidth);
        int height = (int) Math.ceil(textHeight + textSpacing + barHeight);

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textY = -fm.ascent;

        // ---- Draw text ----
        canvas.drawText(buildText(), 0, textY, textPaint);

        // ---- Draw bar ----
        float barTop = textY + fm.descent + textSpacing;
        float barBottom = barTop + barHeight;

        // Background
        rect.set(0, barTop, contentWidth, barBottom);
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, backgroundPaint);

        // Fill
        float filledWidth = contentWidth * usageFraction;
        if (filledWidth > 0f) {
            rect.set(0, barTop, filledWidth, barBottom);
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint);
        }
    }

    // ===== Runtime API =====

    public void setUsage(long used, long total, @NonNull String unit) {
        usedText = String.valueOf(used);
        totalText = String.valueOf(total);
        unitText = unit;

        usageFraction = total <= 0 ? 0f : Math.min(1f, (float) used / total);

        recalculateContentWidth();
        requestLayout();
        invalidate();
    }

    public void setUsageFraction(float fraction) {
        usageFraction = Math.max(0f, Math.min(1f, fraction));
        invalidate();
    }

    // ===== Helpers =====

    private String buildText() {
        return LABEL + usedText + " / " + totalText + " " + unitText;
    }

    private String getOrDefault(@Nullable String value, String fallback) {
        return value != null ? value : fallback;
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private float spToPx() {
        return (float) 11 * getResources().getDisplayMetrics().scaledDensity;
    }
}
