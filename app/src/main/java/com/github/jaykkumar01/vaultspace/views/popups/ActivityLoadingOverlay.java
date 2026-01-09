package com.github.jaykkumar01.vaultspace.views.popups;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

public class ActivityLoadingOverlay {

    private final View overlayView;

    public ActivityLoadingOverlay(Activity activity) {

        FrameLayout root =
                activity.findViewById(android.R.id.content);

        FrameLayout container =
                new FrameLayout(activity);

        container.setLayoutParams(
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        // Dim background (optional, easy to remove)
        container.setBackgroundColor(Color.parseColor("#66000000"));

        // Block touches
        container.setClickable(true);
        container.setFocusable(true);

        // Make sure it's on top
        container.setElevation(1000f);

        ProgressBar progressBar =
                new ProgressBar(activity);

        FrameLayout.LayoutParams progressParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        progressParams.gravity = Gravity.CENTER;

        container.addView(progressBar, progressParams);
        container.setVisibility(View.GONE);

        root.addView(container);
        overlayView = container;
    }

    /* ---------------- Public API ---------------- */

    public void show() {
        overlayView.setVisibility(View.VISIBLE);
    }

    public void hide() {
        overlayView.setVisibility(View.GONE);
    }
}
