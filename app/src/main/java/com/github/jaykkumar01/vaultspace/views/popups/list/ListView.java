package com.github.jaykkumar01.vaultspace.views.popups.list;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.R;

import java.util.List;

public final class ListView extends FrameLayout {

    public interface Callback {
        void onItemSelected(int index);
    }


    private LinearLayout card;

    public ListView(
            @NonNull Context context,
            String title,
            List<String> items,
            Callback callback
    ) {
        super(context);
        init(title, items, callback);
    }

    private void init(
            String title,
            List<String> items,
            Callback callback) {
        /* ---------- Root ---------- */

        setLayoutParams(new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        setBackgroundColor(0x990D1117);
        setClickable(false);

        /* ---------- Card ---------- */

        card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(18), dp(20), dp(20));
        card.setElevation(dp(10));
        card.setClickable(true);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(getContext().getColor(R.color.vs_surface_soft));
        bg.setCornerRadius(dp(16));
        card.setBackground(bg);

        LayoutParams cardParams = new LayoutParams(
                dp(280),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.gravity = Gravity.CENTER;
        card.setLayoutParams(cardParams);

        addView(card);

        /* ---------- Title ---------- */

        if (title != null) {
            TextView titleView = new TextView(getContext());
            titleView.setText(title);
            titleView.setTextSize(16);
            titleView.setTypeface(
                    titleView.getTypeface(),
                    android.graphics.Typeface.BOLD
            );
            titleView.setTextColor(
                    getContext().getColor(R.color.vs_accent_primary)
            );
            titleView.setLines(1);
            titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);

            card.addView(titleView);
            card.addView(accentUnderline());
        }

        /* ---------- Actions ---------- */

        for (int i = 0; i < items.size(); i++) {
            final int index = i;

            TextView action = createActionView(items.get(i));
            action.setOnClickListener(v ->
                    animateOut(() -> callback.onItemSelected(index))
            );

            card.addView(action);
        }


        /* ---------- Entry animation ---------- */

        card.setScaleX(0.9f);
        card.setScaleY(0.9f);
        card.setAlpha(0f);
        card.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(160)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void animateOut(@NonNull Runnable endAction) {
        card.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .alpha(0f)
                .setDuration(120)
                .withEndAction(endAction)
                .start();
    }

    /* ---------- UI helpers ---------- */

    private View accentUnderline() {
        View v = new View(getContext());
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(dp(32), dp(3));
        lp.topMargin = dp(6);
        lp.bottomMargin = dp(12);
        v.setLayoutParams(lp);
        v.setBackgroundColor(
                getContext().getColor(R.color.vs_accent_primary)
        );
        return v;
    }

    private TextView createActionView(String text) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextSize(15);
        tv.setTextColor(
                getContext().getColor(R.color.vs_text_header)
        );
        tv.setPadding(dp(12), dp(14), dp(12), dp(14));
        tv.setClickable(true);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(getContext().getColor(R.color.vs_surface_soft));
        bg.setCornerRadius(dp(10));
        bg.setStroke(
                dp(1),
                getContext().getColor(R.color.vs_accent_primary)
        );
        tv.setBackground(bg);

        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        lp.topMargin = dp(10);
        tv.setLayoutParams(lp);

        return tv;
    }

    /* ---------- Utils ---------- */

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
