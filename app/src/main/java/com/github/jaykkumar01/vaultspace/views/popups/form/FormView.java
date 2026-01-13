package com.github.jaykkumar01.vaultspace.views.popups.form;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.R;
import com.google.android.material.button.MaterialButton;

public final class FormView extends FrameLayout {

    public interface OnSubmit {
        void onSubmit(String value);
    }

    public interface OnCancel {
        void onCancel();
    }

    private final LinearLayout card;
    private final EditText input;

    public FormView(
            @NonNull Context context,
            String title,
            String hint,
            String positiveText,
            OnSubmit onSubmit,
            OnCancel onCancel
    ) {
        super(context);

        setLayoutParams(new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        setClickable(true);
        setBackgroundColor(0x990D1117);

        /* ---------- Card ---------- */

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

        /* ---------- Title ---------- */

        TextView titleView = new TextView(context);
        titleView.setTextSize(18);
        titleView.setTextColor(context.getColor(R.color.vs_text_header));
        titleView.setText(title);

        /* ---------- Input ---------- */

        input = new EditText(context);
        input.setSingleLine(true);
        input.setHint(hint);
        input.setTextColor(context.getColor(R.color.vs_text_header));
        input.setHintTextColor(context.getColor(R.color.vs_text_content));
        input.setBackgroundTintList(
                context.getColorStateList(R.color.vs_toggle_off)
        );

        /* ---------- Actions ---------- */

        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);

        MaterialButton cancelBtn = new MaterialButton(
                context, null,
                com.google.android.material.R.attr.borderlessButtonStyle
        );
        cancelBtn.setText("Cancel");
        cancelBtn.setTextColor(context.getColor(R.color.vs_text_content));

        MaterialButton positiveBtn = new MaterialButton(context);
        positiveBtn.setText(positiveText);
        positiveBtn.setTextColor(context.getColor(R.color.black));
        positiveBtn.setBackgroundTintList(
                context.getColorStateList(R.color.vs_accent_primary)
        );
        positiveBtn.setRippleColorResource(R.color.vs_ripple_primary);

        actions.addView(cancelBtn);
        actions.addView(positiveBtn);

        /* ---------- Assemble ---------- */

        card.addView(titleView);
        card.addView(space());
        card.addView(input);
        card.addView(space());
        card.addView(actions);

        addView(card);

        /* ---------- Interactions ---------- */

        card.setOnClickListener(v -> {});

        setOnClickListener(v ->
                animateOut(onCancel::onCancel)
        );

        cancelBtn.setOnClickListener(v ->
                animateOut(onCancel::onCancel)
        );

        positiveBtn.setOnClickListener(v -> {
            String value = input.getText().toString().trim();
            if (value.isEmpty()) {
                input.setError("Required");
                return;
            }
            animateOut(() -> onSubmit.onSubmit(value));
        });

        /* ---------- Entry animation ---------- */

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

        requestFocusAndShowKeyboard();
    }

    private void animateOut(@NonNull Runnable endAction) {
        hideKeyboard();
        card.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .alpha(0f)
                .setDuration(120)
                .withEndAction(endAction)
                .start();
    }

    /* ---------- Keyboard ---------- */

    private void requestFocusAndShowKeyboard() {
        input.requestFocus();
        post(() -> {
            InputMethodManager imm =
                    (InputMethodManager) getContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm =
                (InputMethodManager) getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(getWindowToken(), 0);
        }
    }

    /* ---------- Utils ---------- */

    private View space() {
        View v = new View(getContext());
        v.setLayoutParams(
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(12)
                )
        );
        return v;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
