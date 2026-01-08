package com.github.jaykkumar01.vaultspace.views;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
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

import com.google.android.material.button.MaterialButton;

public class CreateFolderView extends FrameLayout {

    private static final String TAG = "CreateFolderView";

    public interface Callback {
        void onCreate(String name);
        void onCancel();
    }

    private final LinearLayout card;
    private final TextView titleView;
    private final EditText input;
    private final MaterialButton positiveBtn;

    private Callback callback;
    private String debugOwner = "unknown";

    public CreateFolderView(@NonNull Context context) {
        super(context);

        // Root overlay
        setLayoutParams(new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        setClickable(true);
        setBackgroundColor(Color.TRANSPARENT);

        /* ---------------- Card ---------------- */

        card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(24), dp(20), dp(24), dp(20));
        card.setBackgroundColor(Color.WHITE);
        card.setElevation(dp(8));

        LayoutParams cardParams = new LayoutParams(
                dp(320),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.gravity = Gravity.CENTER;
        card.setLayoutParams(cardParams);

        /* ---------------- Title ---------------- */

        titleView = new TextView(context);
        titleView.setTextSize(18);
        titleView.setTextColor(Color.BLACK);

        /* ---------------- Input ---------------- */

        input = new EditText(context);
        input.setSingleLine(true);

        /* ---------------- Actions ---------------- */

        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);

        MaterialButton cancelBtn = new MaterialButton(context);
        cancelBtn.setText("Cancel");

        positiveBtn = new MaterialButton(context);
        positiveBtn.setText("Create");

        actions.addView(cancelBtn);
        actions.addView(positiveBtn);

        /* ---------------- Assemble ---------------- */

        card.addView(titleView);
        card.addView(input);
        card.addView(actions);

        addView(card);

        // Block touch-through
        card.setOnClickListener(v -> {});

        /* ---------------- Actions ---------------- */

        cancelBtn.setOnClickListener(v -> {
            Log.d(TAG, debugOwner + " → Cancel");
            if (callback != null) callback.onCancel();
            hide();
        });

        positiveBtn.setOnClickListener(v -> {
            String value = input.getText().toString().trim();
            if (value.isEmpty()) {
                input.setError("Required");
                return;
            }
            Log.d(TAG, debugOwner + " → Create: " + value);
            if (callback != null) callback.onCreate(value);
            hide();
        });

        setVisibility(GONE);
    }

    /* ------------------------------------------------
     * Public API
     * ------------------------------------------------ */

    public void show(
            String title,
            String hint,
            String positiveText,
            String debugOwner,
            Callback callback
    ) {
        this.callback = callback;
        this.debugOwner = debugOwner;

        titleView.setText(title);
        input.setHint(hint);
        positiveBtn.setText(positiveText);
        input.setText("");

        Log.d(TAG, debugOwner + " → show()");

        setVisibility(VISIBLE);

        card.setScaleX(0.85f);
        card.setScaleY(0.85f);
        card.setAlpha(0f);

        card.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(180)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        input.requestFocus();
        showKeyboard();
    }

    public void hide() {
        hideKeyboard();

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

    /* ------------------------------------------------
     * Keyboard
     * ------------------------------------------------ */

    private void showKeyboard() {
        post(() -> {
            InputMethodManager imm =
                    (InputMethodManager) getContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm =
                (InputMethodManager) getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
