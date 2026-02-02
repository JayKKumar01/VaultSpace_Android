package com.github.jaykkumar01.vaultspace.album.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.album.helper.DriveResolver;
import com.github.jaykkumar01.vaultspace.album.layout.BandLayout;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.album.model.MediaFrame;
import com.github.jaykkumar01.vaultspace.views.creative.RoundedOutline;

public final class BandViewHolder extends RecyclerView.ViewHolder {

    /* ================= Fields ================= */

    private final TextView timeLabel;
    private final FrameLayout band;
    private final DriveResolver resolver;

    /* ================= Constructor ================= */

    private BandViewHolder(@NonNull LinearLayout root, @NonNull TextView timeLabel, @NonNull FrameLayout band) {
        super(root);
        this.timeLabel = timeLabel;
        this.band = band;
        this.resolver = new DriveResolver(root.getContext());
    }

    /* ================= Creation ================= */

    public static BandViewHolder create(@NonNull ViewGroup parent) {
        Context c = parent.getContext();

        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView label = new TextView(c);
        label.setTextColor(c.getColor(R.color.vs_text_header));
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 6));

        FrameLayout band = new FrameLayout(c);
        band.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        band.setClipToPadding(false);

        root.addView(label);
        root.addView(band);

        return new BandViewHolder(root, label, band);
    }

    /* ================= Bind ================= */

    public void bind(@NonNull BandLayout layout) {
        if (layout.showTimeLabel && layout.timeLabel != null) {
            timeLabel.setText(layout.timeLabel);
            timeLabel.setVisibility(TextView.VISIBLE);
        } else timeLabel.setVisibility(TextView.GONE);

        band.removeAllViews();

        ViewGroup.LayoutParams lp = band.getLayoutParams();
        lp.height = layout.bandHeight;
        band.setLayoutParams(lp);
        band.setRotation(layout.rotationDeg);

        for (MediaFrame f : layout.frames) renderFrame(f);
    }

    /* ================= Media Rendering ================= */

    private void renderFrame(@NonNull MediaFrame f) {
        Context c = band.getContext();
        AlbumMedia m = f.media;

        Log.d("VaultSpace:BandVH",
                "renderFrame fileId=" + m.fileId +
                        " rotation=" + m.rotation +
                        " frame=" + f.width + "x" + f.height +
                        " aspectRatio=" + m.aspectRatio);

        FrameLayout frame = createFrame(c);

        ImageView image = new ImageView(c);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setImageDrawable(AppCompatResources.getDrawable(c, R.drawable.ic_file));

        frame.addView(image, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(f.width, f.height);
        flp.leftMargin = f.baseX;
        flp.topMargin = (band.getLayoutParams().height - f.height) / 2;
        band.addView(frame, flp);

        resolver.resolveAsync(m, path -> {
            if (path == null) return;
            image.post(() -> Glide.with(image)
                    .load(path)
                    .placeholder(AppCompatResources.getDrawable(c, R.drawable.ic_file))
                    .error(AppCompatResources.getDrawable(c, R.drawable.ic_file))
                    .into(image));
        });
    }


    /* ================= Frame Container ================= */

    private static FrameLayout createFrame(Context c) {
        FrameLayout f = new FrameLayout(c);
        f.setBackgroundColor(c.getColor(R.color.vs_surface_soft));
        f.setForeground(AppCompatResources.getDrawable(c, R.drawable.ripple_media));
        f.setClipToOutline(true);
        f.setElevation(dp(c, 2));
        f.setOutlineProvider(new RoundedOutline(dp(c, 14)));
        return f;
    }

    /* ================= Utils ================= */

    private static int dp(Context c, int v) {
        return (int) (v * c.getResources().getDisplayMetrics().density);
    }
}
