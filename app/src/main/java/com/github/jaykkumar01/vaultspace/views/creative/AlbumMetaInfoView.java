package com.github.jaykkumar01.vaultspace.views.creative;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.github.jaykkumar01.vaultspace.R;

public class AlbumMetaInfoView extends AppCompatTextView {

    /* ---------------- Constants ---------------- */

    private static final String PLACEHOLDER = "–";
    private static final String SEPARATOR = " · ";
    private static final String LABEL_CAPTURED = " captured";
    private static final String LABEL_RECORDED = " recorded";

    private static final String LOADING_TEXT =
            PLACEHOLDER + LABEL_CAPTURED + SEPARATOR + PLACEHOLDER + LABEL_RECORDED;

    /* ---------------- State ---------------- */

    private boolean isLoading = true;
    private int capturedCount;
    private int recordedCount;

    /* ---------------- Constructors ---------------- */

    public AlbumMetaInfoView(Context context) {
        super(context);
        init();
    }

    public AlbumMetaInfoView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AlbumMetaInfoView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /* ---------------- Init ---------------- */

    private void init() {
        setTextSize(11);
        setTextColor(getContext().getColor(R.color.vs_brand_text));
        setAlpha(0.75f);
        setGravity(Gravity.CENTER);
        setTypeface(getTypeface(), Typeface.NORMAL);

        render();
    }

    /* ---------------- Public API ---------------- */

    /** Show loading placeholder */
    public void showLoading() {
        if (isLoading) return;

        isLoading = true;
        render();
    }

    /** Update media counts */
    public void setCounts(int captured, int recorded) {
        captured = Math.max(0, captured);
        recorded = Math.max(0, recorded);

        if (!isLoading
                && capturedCount == captured
                && recordedCount == recorded) {
            return;
        }

        isLoading = false;
        capturedCount = captured;
        recordedCount = recorded;
        render();
    }

    /* ---------------- Rendering ---------------- */

    private void render() {
        String text = isLoading ? LOADING_TEXT : buildValueText();
        if (!text.contentEquals(getText())) {
            setText(text);
        }
    }

    private String buildValueText() {
        return capturedCount
                + LABEL_CAPTURED
                + SEPARATOR
                + recordedCount
                + LABEL_RECORDED;
    }
}
