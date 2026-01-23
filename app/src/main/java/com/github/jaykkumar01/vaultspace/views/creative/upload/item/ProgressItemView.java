package com.github.jaykkumar01.vaultspace.views.creative.upload.item;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.utils.ByteFormat;
import com.github.jaykkumar01.vaultspace.views.creative.upload.MultiSegmentProgressBar;

final class ProgressItemView extends FrameLayout {

    private TextView tvName;
    private TextView tvSize;
    private MultiSegmentProgressBar progress;

    ProgressItemView(Context c) {
        super(c);
        init();
    }

    private void init() {
        setClipChildren(false);
        setClipToPadding(false);

        LinearLayout content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        addView(content, new LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(row);

        tvName = new TextView(getContext());
        tvName.setTextSize(12);
        tvName.setMaxLines(1);
        tvName.setEllipsize(TextUtils.TruncateAt.END);
        tvName.setTextColor(
                ContextCompat.getColor(
                        getContext(),
                        R.color.vs_text_content
                )
        );
        row.addView(tvName, new LinearLayout.LayoutParams(
                0, WRAP_CONTENT, 1f
        ));

        tvSize = new TextView(getContext());
        tvSize.setTextSize(12);
        tvSize.setGravity(Gravity.END);
        tvSize.setTextColor(
                ContextCompat.getColor(
                        getContext(),
                        R.color.vs_text_content
                )
        );
        row.addView(tvSize);

        progress = new MultiSegmentProgressBar(getContext());
        LinearLayout.LayoutParams pLp =
                new LinearLayout.LayoutParams(
                        MATCH_PARENT, dp(2)
                );
        pLp.topMargin = dp(4);
        progress.setLayoutParams(pLp);
        progress.setTrackColor(
                ContextCompat.getColor(
                        getContext(),
                        R.color.vs_toggle_off
                )
        );
        progress.setColors(new int[]{
                ContextCompat.getColor(
                        getContext(),
                        R.color.vs_accent_primary
                )
        });
        content.addView(progress);
    }

    @SuppressLint("SetTextI18n")
    boolean render(String fileName, long uploaded, long total) {
        float f = total > 0
                ? Math.min(1f, uploaded / (float) total)
                : 0f;

        if (f >= 1f) return true;

        tvName.setText(fileName != null ? fileName : "Uploading…");
        tvSize.setText(
                ByteFormat.human(uploaded) + " / " +
                        (total > 0 ? ByteFormat.human(total) : "?")
        );

        progress.setFractions(new float[]{ f });
        return false;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
