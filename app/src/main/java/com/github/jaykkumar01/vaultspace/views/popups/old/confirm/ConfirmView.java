package com.github.jaykkumar01.vaultspace.views.popups.old.confirm;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.jaykkumar01.vaultspace.R;
import com.google.android.material.button.MaterialButton;

final class ConfirmView extends FrameLayout {

    ConfirmView(
            Context context,
            String title,
            String message,
            String positiveText,
            boolean showCancel,
            ConfirmRisk risk,
            Runnable onConfirm,
            Runnable onCancel
    ) {
        super(context);

        setLayoutParams(new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        setBackgroundColor(0x990D1117);
        setClickable(true);

        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(18), dp(20), dp(18));
        card.setElevation(dp(10));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(getContext().getColor(R.color.vs_surface_soft));
        bg.setCornerRadius(dp(16));
        card.setBackground(bg);

        LayoutParams cardParams = new LayoutParams(dp(320), ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.gravity = Gravity.CENTER;
        card.setLayoutParams(cardParams);

        TextView titleView = new TextView(getContext());
        titleView.setTextSize(18);
        titleView.setTextColor(getContext().getColor(R.color.vs_text_header));
        titleView.setText(title);

        TextView messageView = new TextView(getContext());
        messageView.setTextSize(14);
        messageView.setTextColor(getContext().getColor(R.color.vs_text_content));
        messageView.setLineSpacing(dp(2), 1f);
        messageView.setText(message);

        LinearLayout actions = new LinearLayout(getContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);

        if (showCancel) {
            MaterialButton cancel = new MaterialButton(
                    getContext(), null,
                    com.google.android.material.R.attr.borderlessButtonStyle
            );
            cancel.setText("Cancel");
            cancel.setOnClickListener(v -> onCancel.run());
            actions.addView(cancel);
        }

        MaterialButton positive = new MaterialButton(getContext());
        positive.setText(positiveText);
        positive.setTextColor(getContext().getColor(R.color.black));

        // ✅ Apply risk-based styling
        applyRiskStyle(positive, risk);

        positive.setOnClickListener(v -> onConfirm.run());
        actions.addView(positive);

        card.addView(titleView);
        card.addView(space(8));
        card.addView(messageView);
        card.addView(space(16));
        card.addView(actions);

        addView(card);

        // Entry animation
        card.setScaleX(0.9f);
        card.setScaleY(0.9f);
        card.setAlpha(0f);
        card.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(180)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    /* ==========================================================
     * Risk styling
     * ========================================================== */

    private void applyRiskStyle(MaterialButton button, ConfirmRisk risk) {
        switch (risk) {
            case RISK_WARNING:
                button.setBackgroundTintList(
                        getContext().getColorStateList(R.color.vs_warning)
                );
                button.setRippleColorResource(R.color.vs_warning_ripple);
                break;

            case RISK_DESTRUCTIVE:
                button.setBackgroundTintList(
                        getContext().getColorStateList(R.color.vs_danger)
                );
                button.setRippleColorResource(R.color.vs_danger_ripple);
                break;

            case RISK_CRITICAL:
                button.setBackgroundTintList(
                        getContext().getColorStateList(R.color.vs_danger_strong)
                );
                button.setRippleColorResource(R.color.vs_danger_ripple);
                break;

            case RISK_NEUTRAL:
            default:
                button.setBackgroundTintList(
                        getContext().getColorStateList(R.color.vs_accent_primary)
                );
                button.setRippleColorResource(R.color.vs_ripple_primary);
                break;
        }
    }

    /* ==========================================================
     * Utils
     * ========================================================== */

    private View space(int dp) {
        View v = new View(getContext());
        v.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(dp)));
        return v;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
