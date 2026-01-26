package com.github.jaykkumar01.vaultspace.setup;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public final class SetupAdapter extends RecyclerView.Adapter<SetupViewHolder> {

    private final List<SetupRow> rows;

    public SetupAdapter(List<SetupRow> rows) {
        this.rows = rows;
    }

    @NonNull
    @Override
    public SetupViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {
        return SetupViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(
            @NonNull SetupViewHolder holder, int position) {
        holder.bind(rows.get(position));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }
}
