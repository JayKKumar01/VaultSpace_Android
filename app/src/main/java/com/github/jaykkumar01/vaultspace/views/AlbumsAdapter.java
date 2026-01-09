package com.github.jaykkumar01.vaultspace.views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.models.AlbumInfo;

import java.util.ArrayList;
import java.util.List;

class AlbumsAdapter extends RecyclerView.Adapter<AlbumsViewHolder>
        implements AlbumItemCallbacks {

    private List<AlbumInfo> items = new ArrayList<>();

    // Normal tap (open album)
    private AlbumsContentView.OnAlbumClickListener clickListener;

    // Overflow / long-press actions
    private AlbumItemCallbacks actionCallbacks;

    AlbumsAdapter() {
        setHasStableIds(true);
    }

    /* ---------------- Callbacks wiring ---------------- */

    void setOnAlbumClickListener(
            AlbumsContentView.OnAlbumClickListener listener
    ) {
        this.clickListener = listener;
    }

    void setAlbumItemCallbacks(AlbumItemCallbacks callbacks) {
        this.actionCallbacks = callbacks;
    }

    /* ---------------- Full submit (DiffUtil) ---------------- */

    void submitAlbums(List<AlbumInfo> data) {
        List<AlbumInfo> newItems =
                data == null ? List.of() : List.copyOf(data);

        DiffUtil.DiffResult diffResult =
                DiffUtil.calculateDiff(
                        new AlbumsDiffCallback(items, newItems)
                );

        items = newItems;
        diffResult.dispatchUpdatesTo(this);
    }

    /* ---------------- Incremental insert ---------------- */

    void addAlbum(AlbumInfo album) {
        if (album == null) return;
        if (containsAlbum(album.id)) return;

        List<AlbumInfo> newItems = new ArrayList<>(items);
        newItems.add(0, album);

        DiffUtil.DiffResult diffResult =
                DiffUtil.calculateDiff(
                        new AlbumsDiffCallback(items, newItems)
                );

        items = List.copyOf(newItems);
        diffResult.dispatchUpdatesTo(this);
    }

    /* ---------------- Targeted updates ---------------- */

    void updateAlbumCover(String albumId, String coverPath) {
        int index = findIndexById(albumId);
        if (index == -1) return;

        AlbumInfo old = items.get(index);

        AlbumInfo updated = new AlbumInfo(
                old.id,
                old.name,
                old.createdTime,
                old.modifiedTime,
                coverPath
        );

        replaceItem(index, updated);
    }

    void updateAlbumName(String albumId, String newName) {
        int index = findIndexById(albumId);
        if (index == -1) return;

        AlbumInfo old = items.get(index);
        if (old.name.equals(newName)) return;

        AlbumInfo updated = new AlbumInfo(
                old.id,
                newName,
                old.createdTime,
                System.currentTimeMillis(),
                old.coverPath
        );

        replaceItem(index, updated);
    }

    void deleteAlbum(String albumId) {
        int index = findIndexById(albumId);
        if (index == -1) return;

        List<AlbumInfo> newItems = new ArrayList<>(items);
        newItems.remove(index);

        DiffUtil.DiffResult diffResult =
                DiffUtil.calculateDiff(
                        new AlbumsDiffCallback(items, newItems)
                );

        items = List.copyOf(newItems);
        diffResult.dispatchUpdatesTo(this);
    }

    /* ---------------- Internal helpers ---------------- */

    private void replaceItem(int index, AlbumInfo updated) {
        List<AlbumInfo> newItems = new ArrayList<>(items);
        newItems.set(index, updated);

        DiffUtil.DiffResult diffResult =
                DiffUtil.calculateDiff(
                        new AlbumsDiffCallback(items, newItems)
                );

        items = List.copyOf(newItems);
        diffResult.dispatchUpdatesTo(this);
    }

    private boolean containsAlbum(String albumId) {
        for (AlbumInfo item : items) {
            if (item.id.equals(albumId)) return true;
        }
        return false;
    }

    private int findIndexById(String albumId) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id.equals(albumId)) return i;
        }
        return -1;
    }

    /* ---------------- AlbumItemCallbacks (forward only) ---------------- */

    @Override
    public void onOverflowClicked(AlbumInfo album) {
        if (actionCallbacks != null) {
            actionCallbacks.onOverflowClicked(album);
        }
    }

    @Override
    public void onLongPressed(AlbumInfo album) {
        if (actionCallbacks != null) {
            actionCallbacks.onLongPressed(album);
        }
    }

    /* ---------------- RecyclerView ---------------- */

    @NonNull
    @Override
    public AlbumsViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album, parent, false);

        return new AlbumsViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(
            @NonNull AlbumsViewHolder holder, int position
    ) {
        AlbumInfo album = items.get(position);
        holder.bind(album);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onAlbumClick(album);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).id.hashCode();
    }
}
