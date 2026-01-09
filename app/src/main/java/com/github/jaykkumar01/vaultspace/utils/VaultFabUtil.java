package com.github.jaykkumar01.vaultspace.utils;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.github.jaykkumar01.vaultspace.R;

public final class VaultFabUtil {

    private VaultFabUtil() {}

    public static ImageButton createAddAlbumFab(Context context) {
        ImageButton fab = new ImageButton(context);

        fab.setImageResource(R.drawable.ic_add_album);
        fab.setBackground(createFabBackground(context));
        fab.setScaleType(ImageButton.ScaleType.FIT_CENTER);
        fab.setElevation(dp(context, 6));
        fab.setClickable(true);
        fab.setFocusable(true);

        int pad = dp(context, 10);
        fab.setPadding(pad, pad, pad, pad);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                dp(context, 56),
                dp(context, 56),
                Gravity.BOTTOM | Gravity.END
        );
        fab.setLayoutParams(params);

        return fab;
    }

    /* ---------------- Internals ---------------- */

    private static LayerDrawable createFabBackground(Context context) {

        // Soft green glow
        GradientDrawable glow = new GradientDrawable();
        glow.setShape(GradientDrawable.OVAL);
        glow.setColor(context.getColor(R.color.vs_brand_text));

        // FAB surface (theme-correct, NOT pure white)
        GradientDrawable fab = new GradientDrawable();
        fab.setShape(GradientDrawable.OVAL);
        fab.setColor(context.getColor(R.color.vs_toggle_off));

        LayerDrawable layers = new LayerDrawable(new GradientDrawable[]{
                glow,
                fab
        });

        int inset = dp(context, 3);
        layers.setLayerInset(1, inset, inset, inset, inset);

        return layers;
    }

    private static int dp(Context context, int v) {
        return (int) (v * context.getResources().getDisplayMetrics().density);
    }
}
