package com.github.jaykkumar01.vaultspace.views.popups.uploadfailures;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.core.session.db.UploadFailureEntity;

import java.io.File;
import java.util.Locale;

public final class UploadFailureVH extends RecyclerView.ViewHolder {

    private final ImageView thumbnail;
    private final ImageView videoBadgeCenter;
    private final ImageView typeBadgeInline;
    private final TextView name;
    private final TextView meta;

    UploadFailureVH(ViewGroup parent) {
        super(new LinearLayout(parent.getContext()));

        Context c = parent.getContext();
        LinearLayout row = (LinearLayout) itemView;
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(c,12), dp(c,10), dp(c,12), dp(c,10));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(c.getColor(R.color.vs_surface_soft));
        bg.setCornerRadius(dp(c,10));
        bg.setStroke(dp(c,1), c.getColor(R.color.vs_media_frame));
        row.setBackground(bg);

        /* ---------- Thumbnail ---------- */

        int thumbSize = dp(c,56);

        FrameLayout thumbWrap = new FrameLayout(c);
        LinearLayout.LayoutParams wrapLp =
                new LinearLayout.LayoutParams(thumbSize, thumbSize);
        wrapLp.rightMargin = dp(c,12);
        thumbWrap.setLayoutParams(wrapLp);

        GradientDrawable thumbBg = new GradientDrawable();
        thumbBg.setColor(c.getColor(R.color.vs_media_sky));
        thumbBg.setCornerRadius(dp(c,10));
        thumbWrap.setBackground(thumbBg);
        thumbWrap.setClipToOutline(true);

        thumbnail = new ImageView(c);
        thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
        thumbnail.setLayoutParams(
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        /* ---------- Center VIDEO badge (≈55%) ---------- */

        int videoBadgeSize = (int) (thumbSize * 0.55f);

        videoBadgeCenter = new ImageView(c);
        videoBadgeCenter.setImageResource(R.drawable.ic_play);
        videoBadgeCenter.setColorFilter(c.getColor(R.color.vs_icon_on_media));
        videoBadgeCenter.setVisibility(View.GONE);

        FrameLayout.LayoutParams videoLp =
                new FrameLayout.LayoutParams(videoBadgeSize, videoBadgeSize);
        videoLp.gravity = Gravity.CENTER;
        videoBadgeCenter.setLayoutParams(videoLp);

        thumbWrap.addView(thumbnail);
        thumbWrap.addView(videoBadgeCenter);

        /* ---------- Text stack ---------- */

        LinearLayout textStack = new LinearLayout(c);
        textStack.setOrientation(LinearLayout.VERTICAL);
        textStack.setLayoutParams(
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        name = new TextView(c);
        name.setTextSize(14);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setTextColor(c.getColor(R.color.vs_text_header));
        name.setMaxLines(1);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);

        /* ---------- Meta row ---------- */

        LinearLayout metaRow = new LinearLayout(c);
        metaRow.setOrientation(LinearLayout.HORIZONTAL);
        metaRow.setGravity(Gravity.CENTER_VERTICAL);

        typeBadgeInline = new ImageView(c);
        LinearLayout.LayoutParams badgeLp =
                new LinearLayout.LayoutParams(dp(c,14), dp(c,14));
        badgeLp.rightMargin = dp(c,6);
        typeBadgeInline.setLayoutParams(badgeLp);
        typeBadgeInline.setVisibility(View.GONE);

        meta = new TextView(c);
        meta.setTextSize(11);
        meta.setLetterSpacing(0.05f);
        meta.setTextColor(c.getColor(R.color.vs_warning));

        metaRow.addView(typeBadgeInline);
        metaRow.addView(meta);

        textStack.addView(name);
        textStack.addView(metaRow);

        row.addView(thumbWrap);
        row.addView(textStack);

        RecyclerView.LayoutParams lp =
                new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        lp.bottomMargin = dp(c,8);
        row.setLayoutParams(lp);
    }

    void bind(UploadFailureEntity e) {
        name.setText(e.displayName);

        String typeUpper = e.type.toUpperCase(Locale.US);
        String ext = resolveExtension(e.displayName);
        meta.setText(ext != null ? typeUpper + " · " + ext : typeUpper);

        bindBadges(typeUpper);

        if (e.thumbnailPath != null) {
            File f = new File(e.thumbnailPath);
            if (f.exists()) {
                Glide.with(thumbnail)
                        .load(f)
                        .centerCrop()
                        .into(thumbnail);
                return;
            }
        }

        thumbnail.setImageResource(R.drawable.ic_media_placeholder);
        thumbnail.setColorFilter(
                itemView.getContext().getColor(R.color.vs_icon_on_media)
        );
    }

    /* ---------- Badge logic ---------- */

    private void bindBadges(String type) {
        videoBadgeCenter.setVisibility(View.GONE);
        typeBadgeInline.setVisibility(View.GONE);

        switch (type) {
            case "VIDEO":
                videoBadgeCenter.setVisibility(View.VISIBLE);
                typeBadgeInline.setImageResource(R.drawable.ic_play_badge);
                typeBadgeInline.setVisibility(View.VISIBLE);
                break;

            case "PHOTO":
                typeBadgeInline.setImageResource(R.drawable.ic_photo);
                typeBadgeInline.setVisibility(View.VISIBLE);
                break;

            case "FILE":
                typeBadgeInline.setImageResource(R.drawable.ic_file);
                typeBadgeInline.setVisibility(View.VISIBLE);
                break;
        }
    }

    private static String resolveExtension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot > 0 && dot < name.length() - 1)
            return name.substring(dot + 1).toUpperCase(Locale.US);
        return null;
    }

    private static int dp(Context c, int v) {
        return (int) (v * c.getResources().getDisplayMetrics().density);
    }
}
