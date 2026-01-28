package com.github.jaykkumar01.vaultspace.views.creative.upload.item;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSelection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ProgressStackAdapter
        extends RecyclerView.Adapter<ProgressStackAdapter.VH> {

    /* ================= State ================= */

    static final class ItemState {
        final UploadSelection selection;
        long uploaded;
        long total;
        boolean failed;

        ItemState(UploadSelection s) {
            this.selection = s;
        }
    }

    private final List<ItemState> items = new ArrayList<>();
    private final Map<String, Integer> indexById = new HashMap<>();

    /* ================= Adapter API ================= */

    ItemState getOrCreate(UploadSelection s) {
        if (s == null) return null;

        Integer idx = indexById.get(s.id);
        if (idx != null) return items.get(idx);

        ItemState st = new ItemState(s);
        items.add(st);
        indexById.put(s.id, items.size() - 1);
        notifyItemInserted(items.size() - 1);
        return st;
    }

    void remove(String id) {
        Integer idx = indexById.remove(id);
        if (idx == null) return;

        items.remove((int) idx);
        notifyItemRemoved(idx);

        // reindex remaining items (small list → cheap & safe)
        for (int i = idx; i < items.size(); i++) {
            indexById.put(items.get(i).selection.id, i);
        }
    }

    void clear() {
        int size = items.size();
        if (size == 0) return;

        items.clear();
        indexById.clear();
        notifyItemRangeRemoved(0, size);
    }

    int indexOfOrMinus(String id) {
        Integer idx = indexById.get(id);
        return idx == null ? -1 : idx;
    }

    /* ================= Recycler ================= */

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ProgressItemView v = new ProgressItemView(parent.getContext());

        RecyclerView.LayoutParams lp =
                new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        lp.bottomMargin = v.dp(6);
        v.setLayoutParams(lp);

        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        h.bind(items.get(pos));
    }

    @Override
    public void onViewRecycled(@NonNull VH holder) {
        holder.reset(); // critical: prevents image/progress bleed
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /* ================= ViewHolder ================= */

    static final class VH extends RecyclerView.ViewHolder {

        private final ProgressItemView v;
        private String boundId;

        VH(@NonNull ProgressItemView v) {
            super(v);
            this.v = v;
        }

        void bind(ItemState st) {
            // 🔒 static bind only when identity changes
            if (!st.selection.id.equals(boundId)) {
                boundId = st.selection.id;
                v.bindStatic(st.selection);
            }

            // 🔁 frequent, cheap updates
            v.bindProgress(st.uploaded, st.total, st.failed);
        }

        void reset() {
            boundId = null;
            v.reset();
        }
    }
}
