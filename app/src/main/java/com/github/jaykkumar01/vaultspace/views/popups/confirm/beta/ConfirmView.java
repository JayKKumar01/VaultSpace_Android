package com.github.jaykkumar01.vaultspace.views.popups.confirm.beta;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.R;
import com.google.android.material.button.MaterialButton;

public final class ConfirmView extends FrameLayout {

    private static final String TAG = "VaultSpace:ConfirmView";

    /* ==========================================================
     * Public Risk Levels
     * ========================================================== */

    public static final int RISK_NEUTRAL = 0;
    public static final int RISK_WARNING = 1;
    public static final int RISK_DESTRUCTIVE = 2;
    public static final int RISK_CRITICAL = 3;

    public ConfirmView(
            @NonNull Context context,
            String title,
            String message,
            boolean showNegative,
            int riskLevel,
            Runnable onPositive,
            Runnable onNegative
    ) {
        super(context);
        init(title, message, showNegative, riskLevel, onPositive, onNegative);
    }

    private void init(
            String title,
            String message,
            boolean showNegative,
            int riskLevel,
            Runnable onPositive,
            Runnable onNegative
    ) {
        /* ---------------- Root ---------------- */

        setLayoutParams(new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        setBackgroundColor(0x990D1117);
        setClickable(true);

        /* ---------------- Card ---------------- */

        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(18), dp(20), dp(18));
        card.setElevation(dp(10));
        card.setClickable(true);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(getContext().getColor(R.color.vs_surface_soft));
        bg.setCornerRadius(dp(16));
        card.setBackground(bg);

        LayoutParams cardParams = new LayoutParams(
                dp(320),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
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

        MaterialButton negativeBtn = new MaterialButton(
                getContext(), null,
                com.google.android.material.R.attr.borderlessButtonStyle
        );
        negativeBtn.setText("Cancel");
        negativeBtn.setTextColor(getContext().getColor(R.color.vs_text_content));
        negativeBtn.setVisibility(showNegative ? VISIBLE : GONE);

        MaterialButton positiveBtn = new MaterialButton(getContext());
        positiveBtn.setText("Confirm");
        positiveBtn.setTextColor(getContext().getColor(R.color.black));

        applyRiskStyle(positiveBtn, riskLevel);

        actions.addView(negativeBtn);
        actions.addView(positiveBtn);

        card.addView(titleView);
        card.addView(space(8));
        card.addView(messageView);
        card.addView(space(16));
        card.addView(actions);

        addView(card);

        /* ---------------- Interactions ---------------- */

        negativeBtn.setOnClickListener(v -> {
            Log.d(TAG, "→ cancel");
            if (onNegative != null) onNegative.run();
        });

        positiveBtn.setOnClickListener(v -> {
            Log.d(TAG, "→ confirm");
            if (onPositive != null) onPositive.run();
        });
    }

    /* ==========================================================
     * Styling
     * ========================================================== */

    private void applyRiskStyle(MaterialButton positiveBtn, int risk) {
        switch (risk) {

            case RISK_WARNING:
                positiveBtn.setBackgroundTintList(
                        getContext().getColorStateList(R.color.vs_warning)
                );
                positiveBtn.setRippleColorResource(R.color.vs_warning_ripple);
                break;

            case RISK_DESTRUCTIVE:
                positiveBtn.setBackgroundTintList(
                        getContext().getColorStateList(R.color.vs_danger)
                );
                positiveBtn.setRippleColorResource(R.color.vs_danger_ripple);
                break;

            case RISK_CRITICAL:
                positiveBtn.setBackgroundTintList(
                        getContext().getColorStateList(R.color.vs_danger_strong)
                );
                positiveBtn.setRippleColorResource(R.color.vs_danger_ripple);
                break;

            case RISK_NEUTRAL:
            default:
                positiveBtn.setBackgroundTintList(
                        getContext().getColorStateList(R.color.vs_accent_primary)
                );
                positiveBtn.setRippleColorResource(R.color.vs_ripple_primary);
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
