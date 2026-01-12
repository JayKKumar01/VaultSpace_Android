package com.github.jaykkumar01.vaultspace.views.popups.loading;

import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

public class LoadingModalView extends FrameLayout {

    public LoadingModalView(Context context) {
        super(context);
        init();
    }

    private void init() {
        // dim background
        setBackgroundColor(0x80000000); // 50% black
        setClickable(true); // block touches

        ProgressBar progressBar = new ProgressBar(getContext());
        LayoutParams lp = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
        );
        lp.gravity = Gravity.CENTER;
        addView(progressBar, lp);
    }
}
