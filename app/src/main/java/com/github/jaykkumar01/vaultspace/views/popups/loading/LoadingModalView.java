package com.github.jaykkumar01.vaultspace.views.popups.loading;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.github.jaykkumar01.vaultspace.R;

public class LoadingModalView extends FrameLayout {

    public LoadingModalView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {

        // THIS view is the overlay
        setLayoutParams(new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        setBackgroundColor(0x990D1117);
        setClickable(true);

        ProgressBar progressBar = new ProgressBar(context);
        progressBar.setIndeterminateTintList(
                ColorStateList.valueOf(
                        context.getColor(R.color.vs_accent_primary)
                )
        );

        LayoutParams lp = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
        );
        lp.gravity = Gravity.CENTER;

        addView(progressBar, lp);
    }
}
