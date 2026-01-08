package com.github.jaykkumar01.vaultspace.views;

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

public class LoadingStateView extends FrameLayout {

    private View pulseView;
    private TextView loadingText;
    private Animation pulseAnimation;

    public LoadingStateView(Context context) {
        super(context);
        init(context);
    }

    public LoadingStateView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LoadingStateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {

        // Root fills parent
        setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));

        // Centered vertical group
        LinearLayout contentGroup = new LinearLayout(context);
        contentGroup.setOrientation(LinearLayout.VERTICAL);
        contentGroup.setGravity(Gravity.CENTER);
        contentGroup.setPadding(dp(24), dp(24), dp(24), dp(24));

        LayoutParams contentParams = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        contentGroup.setLayoutParams(contentParams);

        // Pulse wrapper (breathing room)
        FrameLayout pulseWrapper = new FrameLayout(context);
        LinearLayout.LayoutParams pulseWrapperParams =
                new LinearLayout.LayoutParams(dp(48), dp(48));
        pulseWrapper.setLayoutParams(pulseWrapperParams);

        // Pulse dot
        pulseView = new View(context);
        FrameLayout.LayoutParams pulseParams =
                new FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER);
        pulseView.setLayoutParams(pulseParams);
        pulseView.setBackgroundResource(R.drawable.bg_loading_pulse);
        pulseView.setAlpha(0.9f);

        pulseWrapper.addView(pulseView);

        // Loading text
        loadingText = new TextView(context);
        LinearLayout.LayoutParams textParams =
                new LinearLayout.LayoutParams(
                        LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT
                );
        textParams.topMargin = dp(14);
        loadingText.setLayoutParams(textParams);
        loadingText.setTextSize(13);
        loadingText.setTextColor(
                context.getColor(R.color.vs_text_content)
        );
        loadingText.setAlpha(0.75f);
        loadingText.setText("Loading…");

        // Assemble
        contentGroup.addView(pulseWrapper);
        contentGroup.addView(loadingText);
        addView(contentGroup);

        // Animation
        pulseAnimation = AnimationUtils.loadAnimation(context, R.anim.pulse);
    }

    /* ---------------- Public API ---------------- */

    public void setText(String text) {
        loadingText.setText(text);
    }

    public void start() {
        pulseView.startAnimation(pulseAnimation);
        setVisibility(VISIBLE);
    }

    public void stop() {
        pulseView.clearAnimation();
        setVisibility(GONE);
    }

    /* ---------------- Utils ---------------- */

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stop();
    }
}
