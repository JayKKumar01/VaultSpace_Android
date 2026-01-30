package com.github.jaykkumar01.vaultspace.album.view;

import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.jaykkumar01.vaultspace.album.model.AlbumItem;

import java.util.ArrayList;
import java.util.List;

final class AlbumAdapter
        extends RecyclerView.Adapter<AlbumAdapter.Holder> {

    private final List<AlbumItem> items = new ArrayList<>();

    @NonNull
    @Override
    public Holder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType
    ) {
        FrameLayout card = new FrameLayout(parent.getContext());

        int w = dp(140);
        int h = dp(180);

        RecyclerView.LayoutParams lp =
                new RecyclerView.LayoutParams(w, h);
        card.setLayoutParams(lp);

        // 🔑 REQUIRED so LayoutManager can measure it
        card.setMinimumWidth(w);
        card.setMinimumHeight(h);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFEFEFEF);    // visible light card
        bg.setCornerRadius(dp(12));
        card.setBackground(bg);

        return new Holder(card);
    }

    @Override
    public void onBindViewHolder(
            @NonNull Holder h, int pos
    ) {
        h.bind(items.get(pos));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /* ================= API ================= */

    void submit(List<AlbumItem> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    void addItem(AlbumItem item) {
        items.add(0, item);
        notifyItemInserted(0);
    }

    void release() {
        items.clear();
    }

    /* ================= Holder ================= */

    static final class Holder extends RecyclerView.ViewHolder {

        Holder(@NonNull View v) {
            super(v);
        }

        void bind(AlbumItem item) {
            // intentionally empty
            // layout + transforms are the goal
        }
    }

    /* ================= Utils ================= */

    private static int dp(int v) {
        return Math.round(
                v * Resources.getSystem().getDisplayMetrics().density
        );
    }
}
