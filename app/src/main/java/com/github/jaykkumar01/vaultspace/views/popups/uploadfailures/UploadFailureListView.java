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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.core.session.db.UploadFailureEntity;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public final class UploadFailureListView extends FrameLayout {

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

        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(20), dp(20), dp(18));
        card.setClickable(true);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(getContext().getColor(R.color.vs_surface_soft));
        bg.setCornerRadius(dp(18));
        card.setBackground(bg);

        LayoutParams cp =
                new LayoutParams(dp(320), ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.gravity = Gravity.CENTER;
        addView(card, cp);

        /* ---------- Title ---------- */

        if (title != null) {
            card.addView(titleView(title));
            card.addView(space(8));
            card.addView(subtitleContainer());
            card.addView(space(16));
        }

        /* ---------- List ---------- */

        RecyclerView recycler = new RecyclerView(getContext());
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        recycler.setAdapter(new UploadFailureAdapter(failures));
        recycler.setOverScrollMode(OVER_SCROLL_NEVER);

        int maxHeight = (int) (screenHeightPx() * 0.45f);

        LinearLayout.LayoutParams rvLp =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        recycler.setLayoutParams(rvLp);
        card.addView(recycler);

// 👇 clamp height AFTER layout
        recycler.post(() -> {
            int contentHeight = recycler.computeVerticalScrollRange();
            if (contentHeight > maxHeight) {
                rvLp.height = maxHeight;
                recycler.setLayoutParams(rvLp);
            }
        });



        card.addView(space(18));

        /* ---------- Action ---------- */

        MaterialButton action = new MaterialButton(getContext());
        action.setText(R.string.got_it);
        action.setTextColor(getContext().getColor(R.color.black));
        action.setBackgroundTintList(
                getContext().getColorStateList(R.color.vs_warning)
        );
        action.setRippleColorResource(R.color.vs_warning_ripple);
        action.setOnClickListener(v -> onOk.run());

        LinearLayout.LayoutParams actionLp =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        actionLp.gravity = Gravity.END;
        action.setLayoutParams(actionLp);

        card.addView(action);
    }

    private int screenHeightPx() {
        return getResources().getDisplayMetrics().heightPixels;
    }


    /* ---------------- Adapter ---------------- */

    private static final class UploadFailureAdapter
            extends RecyclerView.Adapter<UploadFailureVH> {

        private final List<UploadFailureEntity> items;

        UploadFailureAdapter(List<UploadFailureEntity> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public UploadFailureVH onCreateViewHolder(
                @NonNull ViewGroup parent,
                int viewType
        ) {
            return new UploadFailureVH(parent);
        }

        @Override
        public void onBindViewHolder(
                @NonNull UploadFailureVH holder,
                int position
        ) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    /* ---------------- UI Bits ---------------- */

    private TextView titleView(String text) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextSize(17);
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        tv.setTextColor(getContext().getColor(R.color.vs_warning));
        return tv;
    }

    private View subtitleContainer() {
        LinearLayout wrap = new LinearLayout(getContext());
        wrap.setOrientation(LinearLayout.HORIZONTAL);

        View accent = new View(getContext());
        LinearLayout.LayoutParams aLp =
                new LinearLayout.LayoutParams(dp(3), ViewGroup.LayoutParams.MATCH_PARENT);
        aLp.rightMargin = dp(8);
        accent.setLayoutParams(aLp);
        accent.setBackgroundColor(getContext().getColor(R.color.vs_warning));

        TextView tv = new TextView(getContext());
        tv.setText(R.string.select_these_items_again_to_continue_uploading);
        tv.setTextSize(13);
        tv.setLineSpacing(dp(2), 1f);
        tv.setTextColor(getContext().getColor(R.color.vs_text_content));

        wrap.addView(accent);
        wrap.addView(tv);

        return wrap;
    }

    /* ---------------- Utils ---------------- */

    private View space(int dp) {
        View v = new View(getContext());
        v.setLayoutParams(
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(dp)
                )
        );
        return v;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
