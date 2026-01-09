package com.github.jaykkumar01.vaultspace.views.states;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;

import com.github.jaykkumar01.vaultspace.R;

public class EmptyStateView extends FrameLayout {

    private ImageView iconView;
    private TextView titleView;
    private TextView subtitleView;
    private Button primaryButton;
    private Button secondaryButton;

    public EmptyStateView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {

        /* ---------------- Root ---------------- */

        setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));

        /* ---------------- Centered content group ---------------- */

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

        /* ---------------- Icon ---------------- */

        iconView = new ImageView(context);
        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(dp(64), dp(64));
        iconView.setLayoutParams(iconParams);
        iconView.setAlpha(0.85f);

        /* ---------------- Title ---------------- */

        titleView = new TextView(context);
        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(
                        LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT
                );
        titleParams.topMargin = dp(16);
        titleView.setLayoutParams(titleParams);
        titleView.setTextSize(16);
        titleView.setTextColor(context.getColor(R.color.vs_text_header));
        titleView.setTypeface(titleView.getTypeface(), Typeface.BOLD);

        /* ---------------- Subtitle ---------------- */

        subtitleView = new TextView(context);
        LinearLayout.LayoutParams subParams =
                new LinearLayout.LayoutParams(
                        LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT
                );
        subParams.topMargin = dp(6);
        subtitleView.setLayoutParams(subParams);
        subtitleView.setTextSize(13);
        subtitleView.setTextColor(context.getColor(R.color.vs_text_content));
        subtitleView.setAlpha(0.8f);

        /* ---------------- CTA container ---------------- */

        LinearLayout ctaContainer = new LinearLayout(context);
        ctaContainer.setOrientation(LinearLayout.VERTICAL);
        ctaContainer.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams ctaParams =
                new LinearLayout.LayoutParams(
                        LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT
                );
        ctaParams.topMargin = dp(20);
        ctaContainer.setLayoutParams(ctaParams);

        /* ---------------- Primary CTA ---------------- */

        primaryButton = createBaseButton(context);
        primaryButton.setBackground(createPrimaryBackground(context));
        primaryButton.setTextColor(context.getColor(R.color.vs_console_bg));

        /* ---------------- Secondary CTA ---------------- */

        secondaryButton = createBaseButton(context);

        LinearLayout.LayoutParams secondaryParams =
                (LinearLayout.LayoutParams) secondaryButton.getLayoutParams();
        secondaryParams.topMargin = dp(10);
        secondaryButton.setLayoutParams(secondaryParams);

        secondaryButton.setBackground(createSecondaryBackground());
        secondaryButton.setTextColor(context.getColor(R.color.vs_text_content));
        secondaryButton.setVisibility(GONE);

        /* ---------------- Assemble ---------------- */

        ctaContainer.addView(primaryButton);
        ctaContainer.addView(secondaryButton);

        contentGroup.addView(iconView);
        contentGroup.addView(titleView);
        contentGroup.addView(subtitleView);
        contentGroup.addView(ctaContainer);

        addView(contentGroup);
    }

    /* ---------------- Button factory ---------------- */

    private Button createBaseButton(Context context) {
        Button button = new Button(context);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LayoutParams.WRAP_CONTENT,
                        dp(30)
                );
        button.setLayoutParams(params);

        button.setMinWidth(dp(120));
        button.setAllCaps(false);
        button.setTextSize(13);
        button.setTypeface(button.getTypeface(), Typeface.BOLD);

        // Adjusted padding for 30dp height
        button.setPadding(dp(12), dp(4), dp(12), dp(4));

        return button;
    }


    /* ---------------- Backgrounds (deterministic) ---------------- */

    private GradientDrawable createPrimaryBackground(Context context) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(context.getColor(R.color.vs_accent_primary));
        drawable.setCornerRadius(dp(6));
        return drawable;
    }

    private GradientDrawable createSecondaryBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(0x00000000); // fully transparent
        drawable.setCornerRadius(dp(6));
        drawable.setStroke(0, 0x00000000); // explicitly no border
        return drawable;
    }

    /* ---------------- Public API ---------------- */

    public void setIcon(@DrawableRes int resId) {
        iconView.setImageResource(resId);
    }

    public void setTitle(String title) {
        titleView.setText(title);
    }

    public void setSubtitle(String subtitle) {
        subtitleView.setText(subtitle);
    }

    public void setPrimaryAction(String text, OnClickListener listener) {
        primaryButton.setText(text);
        primaryButton.setOnClickListener(listener);
    }

    public void setSecondaryAction(String text, OnClickListener listener) {
        secondaryButton.setVisibility(VISIBLE);
        secondaryButton.setText(text);
        secondaryButton.setOnClickListener(listener);
    }

    public void hideSecondaryAction() {
        secondaryButton.setVisibility(GONE);
    }

    /* ---------------- Utils ---------------- */

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
