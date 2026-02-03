package com.github.jaykkumar01.vaultspace.album.view;

import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.github.jaykkumar01.vaultspace.album.layout.BandLayout;
import java.util.ArrayList;
import java.util.List;

public final class AlbumBandAdapter extends RecyclerView.Adapter<BandViewHolder> {

    /* ===== State ===== */

    private final ArrayList<BandLayout> items = new ArrayList<>();
    private OnMediaActionListener listener;

    /* ===== Public APIs ===== */
    public void setMediaActionListener(OnMediaActionListener mediaActionListener) {
        this.listener = mediaActionListener;
    }

    /* Full replace */
    public void setAll(List<BandLayout> newItems) {
        int oldSize = items.size();
        items.clear();
        if (oldSize > 0) notifyItemRangeRemoved(0, oldSize);

        if (newItems == null || newItems.isEmpty()) return;

        items.addAll(newItems);
        notifyItemRangeInserted(0, newItems.size());
    }

    /* Insert contiguous range */
    public void addRange(int start, List<BandLayout> list) {
        if (list == null || list.isEmpty()) return;
        items.addAll(start, list);
        notifyItemRangeInserted(start, list.size());
    }

    /* Remove contiguous range */
    public void removeRange(int start, int count) {
        if (count <= 0) return;
        for (int i = 0; i < count; i++) items.remove(start);
        notifyItemRangeRemoved(start, count);
    }

    /* Replace contiguous range */
    public void replaceRange(int start, int removeCount, List<BandLayout> newItems) {
        if (removeCount > 0) removeRange(start, removeCount);
        if (newItems != null && !newItems.isEmpty())
            addRange(start, newItems);
    }

    /* ===== Adapter ===== */

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public BandViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return BandViewHolder.create(parent,listener);
    }

    @Override
    public void onBindViewHolder(@NonNull BandViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public void onViewRecycled(@NonNull BandViewHolder holder) {
        super.onViewRecycled(holder);
        holder.cancelLoads();
    }
}
