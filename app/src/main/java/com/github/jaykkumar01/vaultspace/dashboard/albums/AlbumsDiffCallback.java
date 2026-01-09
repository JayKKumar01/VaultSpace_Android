package com.github.jaykkumar01.vaultspace.dashboard.albums;

import androidx.recyclerview.widget.DiffUtil;

import com.github.jaykkumar01.vaultspace.models.AlbumInfo;

import java.util.List;
import java.util.Objects;

class AlbumsDiffCallback extends DiffUtil.Callback {

    private final List<AlbumInfo> oldList;
    private final List<AlbumInfo> newList;

    AlbumsDiffCallback(
            List<AlbumInfo> oldList,
            List<AlbumInfo> newList
    ) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(
            int oldItemPosition,
            int newItemPosition
    ) {
        return oldList.get(oldItemPosition).id
                .equals(newList.get(newItemPosition).id);
    }

    @Override
    public boolean areContentsTheSame(
            int oldItemPosition,
            int newItemPosition
    ) {
        AlbumInfo oldItem = oldList.get(oldItemPosition);
        AlbumInfo newItem = newList.get(newItemPosition);

        return oldItem.name.equals(newItem.name)
                && oldItem.modifiedTime == newItem.modifiedTime
                && Objects.equals(oldItem.coverPath, newItem.coverPath);
    }
}
