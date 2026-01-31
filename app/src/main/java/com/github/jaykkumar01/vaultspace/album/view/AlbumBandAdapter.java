package com.github.jaykkumar01.vaultspace.album.view;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.jaykkumar01.vaultspace.album.layout.BandLayout;

import java.util.List;

public final class AlbumBandAdapter extends RecyclerView.Adapter<BandViewHolder> {

    private List<BandLayout> layouts=List.of();

    /* ================= Data ================= */

    public void submitLayouts(List<BandLayout> layouts){
        this.layouts=layouts;
        notifyDataSetChanged();
    }

    /* ================= Adapter ================= */

    @Override
    public int getItemCount(){
        return layouts.size();
    }

    @NonNull
    @Override
    public BandViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,int viewType){
        return BandViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(
            @NonNull BandViewHolder holder,int position){
        holder.bind(layouts.get(position));
    }
}
