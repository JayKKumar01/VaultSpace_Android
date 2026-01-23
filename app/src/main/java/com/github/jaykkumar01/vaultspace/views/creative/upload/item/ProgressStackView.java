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

import java.util.LinkedHashMap;
import java.util.Map;

public final class ProgressStackView extends FrameLayout {

    private final Map<String, ProgressItemView> items =
            new LinkedHashMap<>();

    private LinearLayout container;

    /* ===== Constructors ===== */

    public ProgressStackView(Context context) {
        this(context, null);
    }

    public ProgressStackView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /* ===== Init ===== */

    private void init() {
        setClipChildren(false);
        setClipToPadding(false);
        setElevation(dp(6));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(8));
        bg.setColor(
                ContextCompat.getColor(
                        getContext(),
                        R.color.vs_surface_soft_translucent
                )
        );
        setClickable(false);

        ScrollView scroll = new ScrollView(getContext());
        scroll.setOverScrollMode(OVER_SCROLL_NEVER);
        scroll.setVerticalScrollBarEnabled(false);

        LayoutParams scrollLp =
                new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        scrollLp.gravity = Gravity.BOTTOM;
        scroll.setLayoutParams(scrollLp);

        container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(16), dp(8), dp(16), dp(8));
        container.setBackground(bg);

        scroll.addView(container,
                new LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
        addView(scroll, scrollLp);

        hide();
    }

    /* ===== API ===== */

    public void render(
            String uId,
            String fileName,
            long uploaded,
            long total
    ) {
        ProgressItemView v = items.get(uId);

        if (v == null) {
            v = new ProgressItemView(getContext());

            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(
                            WRAP_CONTENT, WRAP_CONTENT
                    );
            lp.bottomMargin = dp(6); // thin gap
            v.setLayoutParams(lp);

            items.put(uId, v);
            container.addView(v);
        }

        boolean completed = v.render(fileName, uploaded, total);

        if (completed) {
            container.removeView(v);
            items.remove(uId);
        }

        if (items.isEmpty()) hide();
        else if (getVisibility() != VISIBLE) setVisibility(VISIBLE);
    }

    public void hide() {
        if (getVisibility() != GONE) setVisibility(GONE);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    public void reset() {
        items.clear();
        if(container!=null) container.removeAllViews();
        hide();
    }

}
