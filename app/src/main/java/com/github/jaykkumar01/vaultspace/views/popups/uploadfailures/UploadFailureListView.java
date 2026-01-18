package com.github.jaykkumar01.vaultspace.views.popups.uploadfailures;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.core.session.db.UploadFailureEntity;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public final class UploadFailureListView extends FrameLayout {

    private LinearLayout card;

    public UploadFailureListView(
            @NonNull Context context,
            String title,
            List<UploadFailureEntity> failures,
            Runnable onOk
    ) {
        super(context);
        init(title, failures, onOk);
    }

    private void init(
            String title,
            List<UploadFailureEntity> failures,
            Runnable onOk
    ) {
        setLayoutParams(new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        setBackgroundColor(0x990D1117);
        setClickable(true);

        card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(16));
        card.setClickable(true);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(getContext().getColor(R.color.vs_surface_soft));
        bg.setCornerRadius(dp(16));
        card.setBackground(bg);

        LayoutParams cp = new LayoutParams(dp(320), ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.gravity = Gravity.CENTER;
        addView(card, cp);

        if (title != null) {
            TextView tv = titleView(title);
            card.addView(tv);
            card.addView(space(12));
        }

        for (UploadFailureEntity e : failures) {
            card.addView(itemRow(e));
        }

        card.addView(space(16));

        MaterialButton ok = new MaterialButton(getContext());
        ok.setText("OK");
        ok.setOnClickListener(v -> onOk.run());
        card.addView(ok);
    }

    private View itemRow(UploadFailureEntity e) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(getContext().getColor(R.color.vs_surface_soft));
        bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1), getContext().getColor(R.color.vs_accent_primary));
        row.setBackground(bg);

        TextView name = new TextView(getContext());
        name.setText(e.displayName);
        name.setTextSize(14);
        name.setTextColor(getContext().getColor(R.color.vs_text_header));

        TextView type = new TextView(getContext());
        type.setText(e.type);
        type.setTextSize(12);
        type.setTextColor(getContext().getColor(R.color.vs_text_content));

        row.addView(name);
        row.addView(type);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = dp(8);
        row.setLayoutParams(lp);

        return row;
    }

    private TextView titleView(String text) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextSize(16);
        tv.setTextColor(getContext().getColor(R.color.vs_text_header));
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        return tv;
    }

    private View space(int dp) {
        View v = new View(getContext());
        v.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(dp)));
        return v;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
