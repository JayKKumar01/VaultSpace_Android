package com.github.jaykkumar01.vaultspace.views.states;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.github.jaykkumar01.vaultspace.R;

public class FilesContentView extends FrameLayout {

    public FilesContentView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));

        LayoutInflater.from(context)
                .inflate(R.layout.view_mock_content, this, true);

        TextView label = new TextView(context);
        label.setText("Files Content View");
        label.setTextSize(16);
        label.setGravity(Gravity.CENTER);

        addView(label, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));
    }
}
