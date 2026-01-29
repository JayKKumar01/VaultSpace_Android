package com.github.jaykkumar01.vaultspace.views.creative.upload.item;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSelection;

public final class ProgressStackView extends FrameLayout {

    private RecyclerView rv;
    private ProgressStackAdapter adapter;

    /** Single posted scroll runnable (cancel-safe) */
    private Runnable pendingScroll;

    public ProgressStackView(Context c) {
        this(c, null);
    }

    public ProgressStackView(Context c, @Nullable AttributeSet a) {
        super(c, a);
        init();
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }


    private void init() {
        setClipChildren(false);
        setClipToPadding(false);
        setElevation(dp(6));
        setClickable(false);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(8));

        int base = ContextCompat.getColor(
                getContext(), R.color.vs_surface_soft
        );
        float alpha = 0.7f; // 50% (very visible)

        int a = Math.round(alpha * 255f);
        bg.setColor((base & 0x00FFFFFF) | (a << 24));

        setBackground(bg);



        rv = new RecyclerView(getContext());
        rv.setOverScrollMode(OVER_SCROLL_NEVER);
        rv.setClipToPadding(false);
        rv.setPadding(dp(16), dp(8), dp(16), dp(8));

        LinearLayoutManager lm = new LinearLayoutManager(getContext());
        rv.setLayoutManager(lm);


        adapter = new ProgressStackAdapter();
        rv.setAdapter(adapter);

        LayoutParams lp = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        );
        rv.setLayoutParams(lp);
        addView(rv, lp);

        hide();
    }

    /* ================= PUBLIC API ================= */

    public void render(UploadSelection s, long uploaded, long total) {
        if (s == null) return;

        boolean isNew = adapter.indexOfOrMinus(s.id) == -1;

        ProgressStackAdapter.ItemState st = adapter.getOrCreate(s);
        if (st == null) return;

        st.failed = false;
        st.uploaded = uploaded;
        st.total = total;

        // ✅ completion → structural change ONLY
        if (total > 0 && uploaded >= total) {
            cancelPendingScroll();
            adapter.remove(s.id);
            reconcileVisibility();
            return;
        }

        // ✅ update ONLY attached VH (no adapter invalidation)
        int idx = adapter.indexOfOrMinus(s.id);
        if (idx >= 0) {
            RecyclerView.ViewHolder h =
                    rv.findViewHolderForAdapterPosition(idx);
            if (h instanceof ProgressStackAdapter.VH) {
                ((ProgressStackAdapter.VH) h).bind(st);
            }
        }

        // ✅ scroll ONLY when newly inserted
        if (isNew) {
            scheduleScrollToBottom();
        }

        reconcileVisibility();
    }

    public void renderFailure(UploadSelection s) {
        if (s == null) return;

        ProgressStackAdapter.ItemState st = adapter.getOrCreate(s);
        if (st == null) return;

        st.failed = true;

        int idx = adapter.indexOfOrMinus(s.id);
        if (idx >= 0) {
            RecyclerView.ViewHolder h =
                    rv.findViewHolderForAdapterPosition(idx);
            if (h instanceof ProgressStackAdapter.VH) {
                ((ProgressStackAdapter.VH) h).bind(st);
            }
        }

        reconcileVisibility();
    }

    public void reset() {
        cancelPendingScroll();
        adapter.clear();
        hide();
    }

    /* ================= INTERNAL ================= */

    private void scheduleScrollToBottom() {
        cancelPendingScroll();

        pendingScroll = () -> {
            int count = adapter.getItemCount();
            if (count > 0) {
                rv.scrollToPosition(count - 1); // ⚠️ no smoothScroll
            }
        };
        rv.post(pendingScroll);
    }

    private void cancelPendingScroll() {
        if (pendingScroll != null) {
            rv.removeCallbacks(pendingScroll);
            pendingScroll = null;
        }
    }

    private void reconcileVisibility() {
        if (adapter.getItemCount() == 0) hide();
        else if (getVisibility() != VISIBLE) setVisibility(VISIBLE);
    }

    public void hide() {
        if (getVisibility() != GONE) setVisibility(GONE);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
