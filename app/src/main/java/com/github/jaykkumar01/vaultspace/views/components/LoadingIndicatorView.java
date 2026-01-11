package com.github.jaykkumar01.vaultspace.views.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.github.jaykkumar01.vaultspace.R;

public class LoadingIndicatorView extends LinearLayout {

    private View pulseView;
    private TextView loadingText;
    private Animation pulseAnimation;

    public LoadingIndicatorView(Context context) {
        super(context);
        init(context);
    }

    public LoadingIndicatorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LoadingIndicatorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setPadding(dp(24), dp(24), dp(24), dp(24));

        /* ---------------- Pulse ---------------- */

        FrameLayout pulseWrapper = new FrameLayout(context);
        LayoutParams wrapperParams =
                new LayoutParams(dp(48), dp(48));
        pulseWrapper.setLayoutParams(wrapperParams);

        pulseView = new View(context);
        FrameLayout.LayoutParams pulseParams =
                new FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER);
        pulseView.setLayoutParams(pulseParams);
        pulseView.setBackgroundResource(R.drawable.bg_loading_pulse);
        pulseView.setAlpha(0.9f);

        pulseWrapper.addView(pulseView);

        /* ---------------- Text ---------------- */

        loadingText = new TextView(context);
        LayoutParams textParams =
                new LayoutParams(
                        LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT
                );
        textParams.topMargin = dp(14);
        loadingText.setLayoutParams(textParams);
        loadingText.setTextSize(13);
        loadingText.setTextColor(context.getColor(R.color.vs_text_content));
        loadingText.setAlpha(0.75f);
        loadingText.setVisibility(GONE); // important: hidden by default

        /* ---------------- Assemble ---------------- */

        addView(pulseWrapper);
        addView(loadingText);

        pulseAnimation = AnimationUtils.loadAnimation(context, R.anim.pulse);
    }

    /* ---------------- Public API ---------------- */

    public void setText(String text) {
        loadingText.setText(text);
        loadingText.setVisibility(VISIBLE);
    }

    public void clearText() {
        loadingText.setText(null);
        loadingText.setVisibility(GONE);
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);

        if (visibility == VISIBLE) {
            pulseView.startAnimation(pulseAnimation);
        } else {
            pulseView.clearAnimation();
        }
    }

    /* ---------------- Utils ---------------- */

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
