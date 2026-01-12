package com.github.jaykkumar01.vaultspace.views.popups.confirm;

import android.content.Context;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ConfirmView extends LinearLayout {

    public ConfirmView(
            Context context,
            String title,
            String message,
            boolean showNegative,
            Runnable onPositive,
            Runnable onNegative
    ) {
        super(context);
        init(title, message, showNegative, onPositive, onNegative);
    }

    private void init(
            String title,
            String message,
            boolean showNegative,
            Runnable onPositive,
            Runnable onNegative
    ) {
        setOrientation(VERTICAL);
        setPadding(48, 48, 48, 48);
        setGravity(Gravity.CENTER);
        setBackgroundResource(android.R.drawable.dialog_holo_light_frame);

        if (title != null && !title.isEmpty()) {
            TextView titleView = new TextView(getContext());
            titleView.setText(title);
            titleView.setTextSize(18);
            titleView.setGravity(Gravity.CENTER);
            LayoutParams lp = new LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT
            );
            lp.bottomMargin = 24;
            addView(titleView, lp);
        }

        TextView messageView = new TextView(getContext());
        messageView.setText(message);
        messageView.setGravity(Gravity.CENTER);
        LayoutParams msgLp = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
        );
        msgLp.bottomMargin = 32;
        addView(messageView, msgLp);

        LinearLayout buttonRow = new LinearLayout(getContext());
        buttonRow.setOrientation(HORIZONTAL);
        buttonRow.setGravity(Gravity.END);

        if (showNegative) {
            Button negativeBtn = new Button(getContext());
            negativeBtn.setText("Cancel");
            negativeBtn.setOnClickListener(v -> {
                if (onNegative != null) onNegative.run();
            });
            buttonRow.addView(negativeBtn);
        }

        Button positiveBtn = new Button(getContext());
        positiveBtn.setText("OK");
        positiveBtn.setOnClickListener(v -> {
            if (onPositive != null) onPositive.run();
        });
        buttonRow.addView(positiveBtn);

        addView(buttonRow);
    }
}
