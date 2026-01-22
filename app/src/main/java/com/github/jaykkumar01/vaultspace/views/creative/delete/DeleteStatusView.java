package com.github.jaykkumar01.vaultspace.views.creative.delete;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.views.creative.upload.MultiSegmentProgressBar;

public final class DeleteStatusView extends FrameLayout {

    private final TextView title;
    private final TextView cancel;
    private final MultiSegmentProgressBar progress;

    public DeleteStatusView(Context c) {
        super(c);

        /* ================= Container ================= */

        setPadding(dp(16), dp(8), dp(16), dp(8));
        setBackground(createContainerBackground(c));
        setElevation(dp(6));
        setClipToPadding(false);

        /* ================= Root ================= */

        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        addView(root, new LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        /* ================= LEFT ================= */

        LinearLayout left = new LinearLayout(c);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setGravity(Gravity.CENTER_VERTICAL);
        left.setLayoutParams(
                new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        );
        root.addView(left);

        title = new TextView(c);
        title.setTextSize(14);
        title.setMaxLines(1);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setTextColor(
                ContextCompat.getColor(c, R.color.vs_text_header)
        );
        left.addView(title);

        progress = new MultiSegmentProgressBar(c);
        LinearLayout.LayoutParams pLp =
                new LinearLayout.LayoutParams(MATCH_PARENT, dp(4));
        pLp.topMargin = dp(6);
        progress.setLayoutParams(pLp);
        progress.setColors(new int[]{
                ContextCompat.getColor(c, R.color.vs_danger)
        });
        left.addView(progress);

        /* ================= RIGHT ================= */

        cancel = new TextView(c);
        cancel.setText("Cancel");
        cancel.setTextSize(13);
        cancel.setGravity(Gravity.CENTER);
        cancel.setTextColor(
                ContextCompat.getColor(c, R.color.vs_text_header)
        );
        cancel.setPadding(dp(14), dp(6), dp(14), dp(6));
        cancel.setClickable(true);
        cancel.setFocusable(true);
        cancel.setBackground(createCancelBackground(c));

        LinearLayout.LayoutParams cLp =
                new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        cLp.leftMargin = dp(12);
        root.addView(cancel, cLp);
    }

    /* ================= API ================= */

    public void apply(DeleteStatusRenderModel m) {
        title.setText(m.title);
        progress.setFractions(new float[]{ m.progress });

        cancel.setOnClickListener(v -> {
            hide();              // 👈 UI responsibility
            if (m.onCancel != null) {
                m.onCancel.onClick(v);
            }
        });

        if (getVisibility() != VISIBLE) setVisibility(VISIBLE);
    }

    public void hide() {
        setVisibility(GONE);
    }

    /* ================= Styling ================= */

    private GradientDrawable createContainerBackground(Context c) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(10));
        bg.setColor(
                ContextCompat.getColor(c, R.color.vs_surface_soft)
        );
        return bg;
    }

    private GradientDrawable createCancelBackground(Context c) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(8));
        bg.setColor(
                ContextCompat.getColor(c, R.color.vs_danger)
        );
        return bg;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
