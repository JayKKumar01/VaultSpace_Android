package com.github.jaykkumar01.vaultspace.album;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.github.jaykkumar01.vaultspace.R;

public class AlbumContentView extends FrameLayout {

    public AlbumContentView(Context context) {
        super(context);
        init(context);
    }

    public AlbumContentView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AlbumContentView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {

        // Root fills parent
        setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));

        // TEMP placeholder content
        TextView placeholder = new TextView(context);
        LayoutParams params = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        placeholder.setLayoutParams(params);

        placeholder.setText("Album content goes here");
        placeholder.setTextSize(14);
        placeholder.setTextColor(context.getColor(R.color.vs_text_content));
        placeholder.setAlpha(0.6f);

        addView(placeholder);
    }
}
