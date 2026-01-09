package com.github.jaykkumar01.vaultspace.views;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.R;
import com.google.android.material.button.MaterialButton;

public class ConfirmActionView extends FrameLayout {

    private static final String TAG = "VaultSpace:ConfirmActionView";

    /* ---------------- Risk Levels ---------------- */

    public static final int RISK_NEUTRAL = 0;
    public static final int RISK_WARNING = 1;
    public static final int RISK_DESTRUCTIVE = 2;
    public static final int RISK_CRITICAL = 3;

    public interface Callback {
        void onConfirm();
        void onCancel();
    }

    /* ---------------- Views ---------------- */

    private final LinearLayout card;
    private final TextView titleView;
    private final TextView messageView;
    private final MaterialButton positiveBtn;

    private Callback callback;
    private String debugOwner = "unknown";
    private int riskLevel = RISK_NEUTRAL;

    /* ---------------- Constructor ---------------- */

    public ConfirmActionView(@NonNull Context context) {
        super(context);

        setLayoutParams(new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        setClickable(true);
        setBackgroundColor(0x990D1117);

        /* ---------------- Card ---------------- */

        card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(18), dp(20), dp(18));
        card.setElevation(dp(10));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(context.getColor(R.color.vs_surface_soft));
        bg.setCornerRadius(dp(16));
        card.setBackground(bg);

        LayoutParams cardParams = new LayoutParams(
                dp(320),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.gravity = Gravity.CENTER;
        card.setLayoutParams(cardParams);

        /* ---------------- Title ---------------- */

        titleView = new TextView(context);
        titleView.setTextSize(18);
        titleView.setTextColor(context.getColor(R.color.vs_text_header));

        /* ---------------- Message ---------------- */

        messageView = new TextView(context);
        messageView.setTextSize(14);
        messageView.setTextColor(context.getColor(R.color.vs_text_content));
        messageView.setLineSpacing(dp(2), 1f);

        /* ---------------- Actions ---------------- */

        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);

        MaterialButton cancelBtn = new MaterialButton(
                context, null,
                com.google.android.material.R.attr.borderlessButtonStyle
        );
        cancelBtn.setText("Cancel");
        cancelBtn.setTextColor(context.getColor(R.color.vs_text_content));

        positiveBtn = new MaterialButton(context);
        positiveBtn.setTextColor(context.getColor(R.color.black));

        actions.addView(cancelBtn);
        actions.addView(positiveBtn);

        /* ---------------- Assemble ---------------- */

        card.addView(titleView);
        card.addView(space(8));
        card.addView(messageView);
        card.addView(space(16));
        card.addView(actions);

        addView(card);

        /* ---------------- Interactions ---------------- */

        card.setOnClickListener(v -> {});

        setOnClickListener(v -> {
            if (riskLevel == RISK_CRITICAL) return;
            Log.d(TAG, debugOwner + " → outside dismiss");
            if (callback != null) callback.onCancel();
            hide();
        });

        cancelBtn.setOnClickListener(v -> {
            Log.d(TAG, debugOwner + " → cancel");
            if (callback != null) callback.onCancel();
            hide();
        });

        positiveBtn.setOnClickListener(v -> {
            Log.d(TAG, debugOwner + " → confirm");
            if (callback != null) callback.onConfirm();
            hide();
        });

        setVisibility(GONE);
    }

    /* ---------------- Public API ---------------- */

    public void show(
            String title,
            String message,
            String positiveText,
            int riskLevel,
            String debugOwner,
            Callback callback
    ) {
        this.callback = callback;
        this.debugOwner = debugOwner;
        this.riskLevel = riskLevel;

        titleView.setText(title);
        messageView.setText(message);
        positiveBtn.setText(positiveText);

        applyRiskStyle(riskLevel);

        Log.d(TAG, debugOwner + " → show (risk=" + riskLevel + ")");

        setVisibility(VISIBLE);

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

    public void hide() {
        card.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .alpha(0f)
                .setDuration(120)
                .withEndAction(() -> {
                    setVisibility(GONE);
                    callback = null;
                })
                .start();
    }

    public boolean isVisible() {
        return getVisibility() == VISIBLE;
    }

    /* ---------------- Risk Styling ---------------- */

    private void applyRiskStyle(int risk) {
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

    /* ---------------- Utils ---------------- */

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
