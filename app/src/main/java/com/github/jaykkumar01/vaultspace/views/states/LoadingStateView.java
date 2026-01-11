package com.github.jaykkumar01.vaultspace.views.states;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.github.jaykkumar01.vaultspace.views.components.LoadingIndicatorView;

public class LoadingStateView extends FrameLayout {

    private LoadingIndicatorView indicatorView;

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

        setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));

        indicatorView = new LoadingIndicatorView(context);

        LayoutParams params = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        indicatorView.setLayoutParams(params);

        addView(indicatorView);

        // IMPORTANT: start hidden
        setVisibility(GONE);
    }

    /* ---------------- Public API ---------------- */

    public void setText(String text) {
        indicatorView.setText(text);
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        indicatorView.setVisibility(visibility);
    }
}
