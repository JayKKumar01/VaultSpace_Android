package com.github.jaykkumar01.vaultspace.views.creative.upload.item;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadType;
import com.github.jaykkumar01.vaultspace.utils.ByteFormat;
import com.github.jaykkumar01.vaultspace.views.creative.upload.MultiSegmentProgressBar;

final class ProgressItemView extends FrameLayout {

    private ImageView ivThumb, ivOverlay;
    private TextView tvName, tvSize;
    private MultiSegmentProgressBar progress;

    /* ---- cached state (prevents churn) ---- */
    private long lastUploaded = -1;
    private long lastTotal = -1;
    private boolean lastFailed = false;

    ProgressItemView(Context c) {
        super(c);
        init();
    }

    /* ================= Layout ================= */

    private void init() {
        setClipChildren(false);
        setClipToPadding(false);
        setClickable(false);
        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setBaselineAligned(false);
        addView(root, new LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        root.addView(buildThumb());
        root.addView(buildRight(), new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f));
    }

    private FrameLayout buildThumb() {
        FrameLayout c = new FrameLayout(getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(36), dp(36));
        lp.rightMargin = dp(8);
        c.setLayoutParams(lp);

        ivThumb = new ImageView(getContext());
        ivThumb.setLayoutParams(new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        ivThumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ivThumb.setBackground(makeThumbBg());
        ivThumb.setClipToOutline(true);
        c.addView(ivThumb);

        ivOverlay = new ImageView(getContext());
        ivOverlay.setLayoutParams(
                new FrameLayout.LayoutParams(dp(16), dp(16), Gravity.CENTER)
        );
        ivOverlay.setImageResource(R.drawable.ic_play_badge);
        ivOverlay.setVisibility(GONE);
        c.addView(ivOverlay);

        return c;
    }

    private LinearLayout buildRight() {
        LinearLayout r = new LinearLayout(getContext());
        r.setOrientation(LinearLayout.VERTICAL);
        r.setGravity(Gravity.CENTER_VERTICAL);

        tvName = new TextView(getContext());
        tvName.setTextSize(12);
        tvName.setMaxLines(1);
        tvName.setEllipsize(TextUtils.TruncateAt.END);
        tvName.setIncludeFontPadding(false);
        tvName.setTextColor(color(R.color.vs_text_content));
        r.addView(tvName);

        tvSize = new TextView(getContext());
        tvSize.setTextSize(11);
        tvSize.setIncludeFontPadding(false);
        tvSize.setAlpha(0.6f);
        tvSize.setTextColor(color(R.color.vs_text_content));
        r.addView(tvSize);

        progress = new MultiSegmentProgressBar(getContext());
        LinearLayout.LayoutParams pLp =
                new LinearLayout.LayoutParams(MATCH_PARENT, dp(2));
        pLp.topMargin = dp(2);
        progress.setLayoutParams(pLp);
        progress.setTrackColor(color(R.color.vs_toggle_off));
        setNormalColor();
        r.addView(progress);

        return r;
    }

    /* ================= Binding ================= */

    /** Identity-based bind (thumbnail, name, overlay) */
    void bindStatic(UploadSelection s) {
        tvName.setText(s.displayName);
        ivOverlay.setVisibility(s.type == UploadType.VIDEO ? VISIBLE : GONE);

        Glide.with(ivThumb)
                .load(s.thumbnailPath)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .placeholder(R.drawable.ic_file)
                .error(R.drawable.ic_file)
                .dontAnimate()
                .into(ivThumb);
    }

    /** Hot path — called VERY frequently */
    @SuppressLint("SetTextI18n")
    void bindProgress(long uploaded, long total, boolean failed) {

        // 🔒 nothing changed → do nothing
        if (uploaded == lastUploaded &&
                total == lastTotal &&
                failed == lastFailed) {
            return;
        }

        lastUploaded = uploaded;
        lastTotal = total;
        lastFailed = failed;

        if (failed) {
            progress.setColors(new int[]{color(R.color.vs_warning)});
            progress.setFractions(new float[]{1f});
            return;
        }

        setNormalColor();

        float f = total > 0 ? Math.min(1f, uploaded / (float) total) : 0f;
        progress.setFractions(new float[]{f});

        tvSize.setText(
                ByteFormat.human(uploaded) + " / " +
                        (total > 0 ? ByteFormat.human(total) : "?")
        );
    }

    /** Called ONLY when recycled */
    void reset() {
        lastUploaded = -1;
        lastTotal = -1;
        lastFailed = false;

        tvSize.setText(null);
        ivOverlay.setVisibility(GONE);
        setNormalColor();
        progress.setFractions(new float[]{0f});
    }

    /* ================= Helpers ================= */

    private void setNormalColor() {
        progress.setColors(new int[]{color(R.color.vs_accent_primary)});
    }

    private int color(int id) {
        return ContextCompat.getColor(getContext(), id);
    }

    private GradientDrawable makeThumbBg() {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(6));
        d.setColor(color(R.color.vs_surface_soft));
        return d;
    }

    int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
