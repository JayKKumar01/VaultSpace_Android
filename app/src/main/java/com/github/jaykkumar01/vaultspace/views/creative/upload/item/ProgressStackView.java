package com.github.jaykkumar01.vaultspace.views.creative.upload.item;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSelection;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ProgressStackView extends FrameLayout {

    private final Map<String, ProgressItemView> items = new LinkedHashMap<>();
    private LinearLayout container;

    public ProgressStackView(Context c) {
        this(c, null);
    }

    public ProgressStackView(Context c, @Nullable AttributeSet a) {
        super(c, a);
        init();
    }

    private void init() {
        setClipChildren(false);
        setClipToPadding(false);
        setElevation(dp(6));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(8));
        bg.setColor(ContextCompat.getColor(getContext(), R.color.vs_surface_soft_translucent));
        setClickable(false);

        ScrollView scroll = new ScrollView(getContext());
        scroll.setOverScrollMode(OVER_SCROLL_NEVER);
        scroll.setVerticalScrollBarEnabled(true);
        scroll.setScrollbarFadingEnabled(true);
        scroll.setScrollBarStyle(SCROLLBARS_INSIDE_OVERLAY);

        LayoutParams slp = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        slp.gravity = Gravity.BOTTOM;
        scroll.setLayoutParams(slp);

        container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(16), dp(8), dp(16), dp(8));
        container.setBackground(bg);

        scroll.addView(container, new LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
        addView(scroll, slp);
        hide();
    }

    /* ================= API ================= */

    public void render(UploadSelection selection, long uploaded, long total) {
        ProgressItemView v = getOrCreate(selection);
        if (v == null) return;

        if (v.update(uploaded, total)) {
            container.removeView(v);
            items.remove(selection.id);
        }

        reconcileVisibility();
    }

    public void renderFailure(UploadSelection selection) {
        ProgressItemView v = getOrCreate(selection);
        if (v == null) return;

        v.renderFailure();
        reconcileVisibility();
    }

    public void reset() {
        items.clear();
        if (container != null) container.removeAllViews();
        hide();
    }

    /* ================= Internals ================= */

    private ProgressItemView getOrCreate(UploadSelection selection) {
        if (selection == null) return null;

        String id = selection.id;
        ProgressItemView v = items.get(id);
        if (v != null) return v;

        v = new ProgressItemView(getContext());
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        lp.bottomMargin = dp(6);
        v.setLayoutParams(lp);
        v.bind(selection);

        items.put(id, v);
        container.addView(v);
        return v;
    }

    private void reconcileVisibility() {
        if (items.isEmpty()) hide();
        else if (getVisibility() != VISIBLE) setVisibility(VISIBLE);
    }

    public void hide() {
        if (getVisibility() != GONE) setVisibility(GONE);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
