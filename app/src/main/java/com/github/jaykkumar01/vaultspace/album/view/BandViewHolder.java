package com.github.jaykkumar01.vaultspace.album.view;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.album.band.TimeBucketizer;
import com.github.jaykkumar01.vaultspace.album.helper.DriveResolver;
import com.github.jaykkumar01.vaultspace.album.layout.BandLayout;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.album.model.MediaFrame;

import java.util.ArrayList;
import java.util.Locale;

public final class BandViewHolder extends RecyclerView.ViewHolder {

    /* ================= Fields ================= */

    private final ArrayList<String> activeMediaIds = new ArrayList<>(2);

    private final TextView timeLabel;
    private final FrameLayout band;
    private final DriveResolver resolver;
    private final OnMediaActionListener listener;

    /* ================= Constructor ================= */

    private BandViewHolder(@NonNull LinearLayout root,
                           @NonNull TextView timeLabel,
                           @NonNull FrameLayout band,
                           @NonNull OnMediaActionListener listener) {
        super(root);
        this.timeLabel = timeLabel;
        this.band = band;
        this.listener = listener;
        this.resolver = new DriveResolver(root.getContext());
    }

    /* ================= Creation ================= */

    public static BandViewHolder create(@NonNull ViewGroup parent, OnMediaActionListener listener) {
        Context c = parent.getContext();

        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView label = createHeaderLabel(c);
        FrameLayout band = createBandContainer(c);

        root.addView(label);
        root.addView(band);

        return new BandViewHolder(root, label, band, listener);
    }

    /* ================= Bind ================= */

    public void bind(@NonNull BandLayout layout) {
        activeMediaIds.clear();
        bindHeader(layout);
        band.removeAllViews();

        for (MediaFrame f : layout.frames)
            renderFrame(f, layout.timeLabel, layout.rotationDeg);
    }

    private void bindHeader(BandLayout layout) {
        if (layout.showTimeLabel && layout.timeLabel != null) {
            timeLabel.setText(layout.timeLabel);
            timeLabel.setVisibility(TextView.VISIBLE);
        } else timeLabel.setVisibility(TextView.GONE);
    }

    /* ================= Media Rendering ================= */

    private void renderFrame(@NonNull MediaFrame f, String timeLabel, float rotationDeg) {
        Context c = band.getContext();
        AlbumMedia m = f.media;

        // 1️⃣ Card (physical object)
        CardView card = createCard(c, f, rotationDeg);

        // 2️⃣ Content (visuals)
        LinearLayout content = createCardContent(c);
        ConstraintLayout frame = createRatioFrame(c);
        ImageView image = createImageView(c);
        TextView time = createTimeView(c, timeLabel, m.momentMillis);

        frame.addView(image, createRatioLayoutParams(f));
        if (m.isVideo) {
            frame.addView(createVideoOverlay(c));

            TextView dur = createDurationBadge(c, m.durationMillis);
            if (dur != null) frame.addView(dur);
        }


        content.addView(frame);
        content.addView(time);

        // 4️⃣ Assemble
        card.addView(content);
        band.addView(card);

        // 5️⃣ Click callbacks
        card.setOnClickListener(v -> {
            if (listener != null) listener.onMediaClick(m);
        });

        card.setOnLongClickListener(v -> {
            if (listener != null) listener.onMediaLongPress(m);
            return true;
        });

        // 6️⃣ Image loading
        activeMediaIds.add(m.fileId);
        loadImageAsync(c, image, m);
    }


    /* ================= Factories ================= */

    private static TextView createHeaderLabel(Context c) {
        TextView t = new TextView(c);
        t.setTextColor(c.getColor(R.color.vs_text_header));
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 6));
        return t;
    }

    private static FrameLayout createBandContainer(Context c) {
        FrameLayout f = new FrameLayout(c);
        f.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        f.setClipToPadding(false);
        return f;
    }

    private static CardView createCard(Context c, MediaFrame f, float rotationDeg) {
        CardView card = new CardView(c);

        card.setCardBackgroundColor(c.getColor(R.color.vs_surface_soft));
        card.setRadius(dp(c, 4));
        card.setCardElevation(dp(c, 4));
        card.setUseCompatPadding(true);
        card.setClickable(true);
        card.setFocusable(true);
        card.setLongClickable(true);

        TypedValue out = new TypedValue();
        c.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, out, true);
        card.setForeground(AppCompatResources.getDrawable(c, out.resourceId));

        FrameLayout.LayoutParams lp =
                new FrameLayout.LayoutParams(f.width, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = f.baseX;
        lp.topMargin = dp(c, 6);
        lp.gravity = Gravity.CENTER_VERTICAL;
        card.setLayoutParams(lp);

        // ✅ rotation stays EXACTLY like this
        card.setLayerType(CardView.LAYER_TYPE_HARDWARE, null);
        card.setRotation(rotationDeg);
        card.setRotationX(0.4f);
        card.setRotationY(-0.4f);
        card.setCameraDistance(8000 * c.getResources().getDisplayMetrics().density);

        return card;
    }


    private static LinearLayout createCardContent(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(c, 8), dp(c, 8), dp(c, 8), 0);
        return l;
    }

    private static ConstraintLayout createRatioFrame(Context c) {
        ConstraintLayout cl = new ConstraintLayout(c);
        cl.setBackgroundColor(c.getColor(R.color.vs_surface_soft));
        cl.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return cl;
    }

    private static ConstraintLayout.LayoutParams createRatioLayoutParams(MediaFrame f) {
        ConstraintLayout.LayoutParams lp =
                new ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        0
                );


        float ar = (float) f.width / f.height;

        lp.dimensionRatio = ar >= 1f
                ? (int) (ar * 1000) + ":1000"
                : "1000:" + (int) (1000 / ar);


        lp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        lp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        lp.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        lp.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        return lp;
    }

    private static ImageView createImageView(Context c) {
        ImageView i = new ImageView(c);
        i.setScaleType(ImageView.ScaleType.CENTER_CROP);
        i.setImageDrawable(AppCompatResources.getDrawable(c, R.drawable.ic_photo));
        return i;
    }

    private static FrameLayout createVideoOverlay(Context c) {
        FrameLayout overlay = new FrameLayout(c);

        // IMPORTANT: use 0dp + constraints (ConstraintLayout rule)
        ConstraintLayout.LayoutParams lp =
                new ConstraintLayout.LayoutParams(
                        0,
                        0
                );
        lp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        lp.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        lp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        lp.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;

        overlay.setLayoutParams(lp);
        overlay.setClickable(false);
        overlay.setFocusable(false);

        // Subtle scrim
        overlay.setBackgroundColor(0x26000000); // ~15% black

        ImageView play = new ImageView(c);
        play.setImageDrawable(AppCompatResources.getDrawable(c, R.drawable.ic_play_badge));

        FrameLayout.LayoutParams plp =
                new FrameLayout.LayoutParams(
                        dp(c, 32),
                        dp(c, 32),
                        Gravity.CENTER
                );

        overlay.addView(play, plp);
        return overlay;
    }


    private static TextView createTimeView(Context c, String timeLabel, long millis) {
        TextView t = new TextView(c);
        t.setText(TimeBucketizer.formatTimeForKey(c, timeLabel, millis));
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        t.setTextColor(c.getColor(R.color.vs_text_content));
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(c, 6), 0, dp(c, 8));
        return t;
    }

    private static TextView createDurationBadge(Context c, long durationMs) {
        String txt = formatDuration(durationMs);
        if (txt == null) return null;

        TextView t = new TextView(c);
        t.setText(txt);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        t.setTextColor(0xFFFFFFFF);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(dp(c, 6), dp(c, 2), dp(c, 6), dp(c, 2));

        t.setBackground(AppCompatResources.getDrawable(c, R.drawable.bg_duration_badge));
        t.setClickable(false);
        t.setFocusable(false);

        ConstraintLayout.LayoutParams lp =
                new ConstraintLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        lp.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        lp.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        lp.rightMargin = dp(c, 6);
        lp.bottomMargin = dp(c, 6);

        t.setLayoutParams(lp);
        return t;
    }



    private void loadImageAsync(Context c, ImageView image, AlbumMedia m) {
        Drawable placeholder = AppCompatResources.getDrawable(c, m.isVideo ? R.drawable.ic_play_badge : R.drawable.ic_photo);
        image.setTag(m.fileId);
        resolver.resolveAsync(m, path -> {
            if (path == null) return;
            image.post(() -> {
                if (!m.fileId.equals(image.getTag())) return;
                Glide.with(image)
                        .load(path)
                        .placeholder(placeholder)
                        .error(placeholder)
                        .into(image);
            });
        });
    }

    public void cancelLoads() {
        for (int i = 0, n = activeMediaIds.size(); i < n; i++)
            resolver.cancel(activeMediaIds.get(i));
        activeMediaIds.clear();
    }


    /* ================= Utils ================= */

    private static String formatDuration(long ms) {
        if (ms <= 0) return null;
        long totalSec = ms / 1000;
        long s = totalSec % 60;
        long m = (totalSec / 60) % 60;
        long h = totalSec / 3600;

        return h > 0
                ? String.format(Locale.US, "%d:%02d:%02d", h, m, s)
                : String.format(Locale.US, "%d:%02d", m, s);
    }


    private static int dp(Context c, int v) {
        return (int) (v * c.getResources().getDisplayMetrics().density);
    }
}
