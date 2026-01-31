package com.github.jaykkumar01.vaultspace.album.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.album.layout.BandLayout;
import com.github.jaykkumar01.vaultspace.album.model.MediaFrame;

public final class BandViewHolder extends RecyclerView.ViewHolder {

    private final TextView timeLabel;
    private final FrameLayout band;

    private BandViewHolder(
            @NonNull LinearLayout root,
            @NonNull TextView timeLabel,
            @NonNull FrameLayout band
    ) {
        super(root);
        this.timeLabel = timeLabel;
        this.band = band;
    }

    /* ============================================================
       Creation
       ============================================================ */

    public static BandViewHolder create(@NonNull ViewGroup parent) {
        Context c = parent.getContext();

        // Root container (vertical)
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // Time label (outside band geometry)
        TextView label = new TextView(c);
        label.setTextColor(c.getColor(R.color.vs_text_header));
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 6));

        // Band container (pure geometry)
        FrameLayout band = new FrameLayout(c);
        band.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        band.setClipToPadding(false);

        root.addView(label);
        root.addView(band);

        return new BandViewHolder(root, label, band);
    }

    /* ============================================================
       Bind (RENDER ONLY)
       ============================================================ */

    public void bind(@NonNull BandLayout layout) {

        // ----- Time label -----
        if (layout.timeLabel != null && !layout.timeLabel.isEmpty()) {
            timeLabel.setText(layout.timeLabel);
            timeLabel.setVisibility(TextView.VISIBLE);
        } else {
            timeLabel.setVisibility(TextView.GONE);
        }

        // ----- Band geometry -----
        band.removeAllViews();

        ViewGroup.LayoutParams bandLp = band.getLayoutParams();
        bandLp.height = layout.bandHeight;
        band.setLayoutParams(bandLp);

        // Render frames
        for (MediaFrame f : layout.frames) {
            FrameLayout frame = createMediaFrame(band.getContext());

            FrameLayout.LayoutParams flp =
                    new FrameLayout.LayoutParams(f.width, f.height);
            flp.leftMargin = f.baseX;
            flp.topMargin = (layout.bandHeight - f.height) / 2;

            band.addView(frame, flp);
        }
    }

    /* ============================================================
       Media Frame
       ============================================================ */

    private static FrameLayout createMediaFrame(Context c) {
        FrameLayout frame = new FrameLayout(c);
        frame.setBackgroundColor(c.getColor(R.color.vs_surface_soft));
        frame.setForeground(c.getDrawable(R.drawable.ripple_media));
        frame.setClipToOutline(true);
        frame.setElevation(dp(c, 2));
        frame.setOutlineProvider(new RoundedOutline(dp(c, 14)));
        return frame;
    }

    /* ============================================================
       Utils
       ============================================================ */

    private static int dp(Context c, int v) {
        return (int) (v * c.getResources().getDisplayMetrics().density);
    }
}
