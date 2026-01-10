package com.github.jaykkumar01.vaultspace.views.popups;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.R;
import com.google.android.material.button.MaterialButton;

public final class BlockingOverlayView extends FrameLayout {

    private static final String TAG = "VaultSpace:BlockingOverlay";

    /* ==========================================================
     * Risk Levels (owned here)
     * ========================================================== */

    public static final int RISK_NEUTRAL = 0;
    public static final int RISK_WARNING = 1;
    public static final int RISK_DESTRUCTIVE = 2;
    public static final int RISK_CRITICAL = 3;


    /* ==========================================================
     * Internal state model
     * ========================================================== */

    private enum DesiredIntent {
        NONE,
        LOADING
    }

    private enum ActiveMode {
        NONE,
        LOADING,
        CONFIRM
    }

    private DesiredIntent desiredIntent = DesiredIntent.NONE;
    private ActiveMode activeMode = ActiveMode.NONE;

    /* ==========================================================
     * Confirm API
     * ========================================================== */

    public interface ConfirmCallback {
        void onConfirm();
        void onCancel();
    }

    /* ==========================================================
     * Views
     * ========================================================== */

    private final View dimBackground;

    private final FrameLayout loadingLayer;
    private final ProgressBar progressBar;

    private final FrameLayout confirmLayer;
    private final LinearLayout confirmCard;
    private final TextView titleView;
    private final TextView messageView;
    private final MaterialButton positiveBtn;

    private ConfirmCallback confirmCallback;
    private int riskLevel;
    private String debugOwner = "unknown";

    /* ==========================================================
     * Attachment
     * ========================================================== */

    public static BlockingOverlayView attach(@NonNull Activity activity) {
        ViewGroup root = activity.findViewById(android.R.id.content);
        BlockingOverlayView view = new BlockingOverlayView(activity);
        root.addView(view);
        Log.d(TAG, "Attached to activity root");
        return view;
    }

    /* ==========================================================
     * Constructor
     * ========================================================== */

    private BlockingOverlayView(@NonNull Context context) {
        super(context);

        setLayoutParams(new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        setVisibility(GONE);
        setClickable(true);

        /* ---------------- Shared dim background ---------------- */

        dimBackground = new View(context);
        dimBackground.setLayoutParams(match());
        dimBackground.setBackgroundColor(0x990D1117);
        dimBackground.setClickable(true);

        /* ---------------- Loading layer ---------------- */

        loadingLayer = new FrameLayout(context);
        loadingLayer.setLayoutParams(match());
        loadingLayer.setVisibility(GONE);

        progressBar = new ProgressBar(context);
        LayoutParams progressParams = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        progressParams.gravity = Gravity.CENTER;
        loadingLayer.addView(progressBar, progressParams);

        /* ---------------- Confirm layer ---------------- */

        confirmLayer = new FrameLayout(context);
        confirmLayer.setLayoutParams(match());
        confirmLayer.setVisibility(GONE);

        confirmCard = new LinearLayout(context);
        confirmCard.setOrientation(LinearLayout.VERTICAL);
        confirmCard.setPadding(dp(20), dp(18), dp(20), dp(18));
        confirmCard.setElevation(dp(10));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(context.getColor(R.color.vs_surface_soft));
        bg.setCornerRadius(dp(16));
        confirmCard.setBackground(bg);

        LayoutParams cardParams = new LayoutParams(
                dp(320),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.gravity = Gravity.CENTER;
        confirmCard.setLayoutParams(cardParams);

        titleView = new TextView(context);
        titleView.setTextSize(18);
        titleView.setTextColor(context.getColor(R.color.vs_text_header));

        messageView = new TextView(context);
        messageView.setTextSize(14);
        messageView.setTextColor(context.getColor(R.color.vs_text_content));
        messageView.setLineSpacing(dp(2), 1f);

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

        confirmCard.addView(titleView);
        confirmCard.addView(space(8));
        confirmCard.addView(messageView);
        confirmCard.addView(space(16));
        confirmCard.addView(actions);

        confirmLayer.addView(confirmCard);

        /* ---------------- Assemble ---------------- */

        addView(dimBackground);
        addView(loadingLayer);
        addView(confirmLayer);

        /* ---------------- Interactions ---------------- */

        confirmCard.setOnClickListener(v -> {});

        confirmLayer.setOnClickListener(v -> {
            if (riskLevel == RISK_CRITICAL) return;
            Log.d(TAG, debugOwner + " → outside dismiss");
            if (confirmCallback != null) confirmCallback.onCancel();
            dismissConfirmInternal();
        });

        cancelBtn.setOnClickListener(v -> {
            Log.d(TAG, debugOwner + " → cancel");
            if (confirmCallback != null) confirmCallback.onCancel();
            dismissConfirmInternal();
        });

        positiveBtn.setOnClickListener(v -> {
            Log.d(TAG, debugOwner + " → confirm");
            if (confirmCallback != null) confirmCallback.onConfirm();
            dismissConfirmInternal();
        });
    }

    /* ==========================================================
     * Public API
     * ========================================================== */

    public void requestLoading() {
        desiredIntent = DesiredIntent.LOADING;
        reconcile();
    }

    public void clearLoading() {
        desiredIntent = DesiredIntent.NONE;
        reconcile();
    }

    public void showConfirm(
            String title,
            String message,
            String positiveText,
            int riskLevel,
            String debugOwner,
            ConfirmCallback callback
    ) {
        if (activeMode == ActiveMode.CONFIRM) return;

        this.confirmCallback = callback;
        this.debugOwner = debugOwner;
        this.riskLevel = riskLevel;

        titleView.setText(title);
        messageView.setText(message);
        positiveBtn.setText(positiveText);
        applyRiskStyle(riskLevel);

        activeMode = ActiveMode.CONFIRM;
        reconcile();

        confirmCard.setScaleX(0.9f);
        confirmCard.setScaleY(0.9f);
        confirmCard.setAlpha(0f);

        confirmCard.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(180)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    public void dismissConfirm() {
        if (activeMode != ActiveMode.CONFIRM) return;
        dismissConfirmInternal();
    }


    public void reset() {
        desiredIntent = DesiredIntent.NONE;
        activeMode = ActiveMode.NONE;
        reconcile();
    }

    public boolean isConfirmVisible() {
        return activeMode == ActiveMode.CONFIRM;
    }

    public boolean isBlocking() {
        return activeMode != ActiveMode.NONE;
    }

    /* ==========================================================
     * Internal reconciliation
     * ========================================================== */

    private void dismissConfirmInternal() {
        confirmCard.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .alpha(0f)
                .setDuration(120)
                .withEndAction(() -> {
                    confirmCallback = null;
                    activeMode = ActiveMode.NONE;
                    reconcile();
                })
                .start();
    }

    private void reconcile() {

        if (activeMode == ActiveMode.CONFIRM) {
            setVisibility(VISIBLE);
            loadingLayer.setVisibility(GONE);
            confirmLayer.setVisibility(VISIBLE);
            return;
        }

        if (desiredIntent == DesiredIntent.LOADING) {
            activeMode = ActiveMode.LOADING;
            setVisibility(VISIBLE);
            loadingLayer.setVisibility(VISIBLE);
            confirmLayer.setVisibility(GONE);
            return;
        }

        activeMode = ActiveMode.NONE;
        loadingLayer.setVisibility(GONE);
        confirmLayer.setVisibility(GONE);
        setVisibility(GONE);
    }

    /* ==========================================================
     * Styling utils
     * ========================================================== */

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


    private LayoutParams match() {
        return new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
    }

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
