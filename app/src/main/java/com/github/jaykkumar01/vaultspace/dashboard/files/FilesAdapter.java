package com.github.jaykkumar01.vaultspace.dashboard.files;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.models.FileNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class FilesAdapter extends RecyclerView.Adapter<FileViewHolder> {

    /* ================= Data ================= */

    private final List<FileNode> files = new ArrayList<>();
    private final FilesContentView.OnItemInteractionListener listener;

    /* ================= Sorting ================= */

    private final Comparator<FileNode> DEFAULT_COMPARATOR = (a, b) -> {
        if (a.isFolder && !b.isFolder) return -1;
        if (!a.isFolder && b.isFolder) return 1;
        return a.name.compareToIgnoreCase(b.name);
    };

    public FilesAdapter(FilesContentView.OnItemInteractionListener listener) {
        this.listener = listener;
        setHasStableIds(true);
    }

    /* ================= Stable IDs ================= */

    @Override
    public long getItemId(int position) {
        return files.get(position).id.hashCode();
    }

    void submitList(List<FileNode> newList) {
        List<FileNode> sorted = new ArrayList<>();
        if (newList != null) sorted.addAll(newList);
        sorted.sort(DEFAULT_COMPARATOR);

        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffCallback(files, sorted));
        files.clear();
        files.addAll(sorted);
        diff.dispatchUpdatesTo(this);
    }

    void addFile(FileNode node) {
        int position = findInsertPosition(node);
        files.add(position, node);
        notifyItemInserted(position);
    }

    void removeFile(String id) {
        for (int i = 0; i < files.size(); i++) {
            if (files.get(i).id.equals(id)) {
                files.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    void updateFile(FileNode node) {
        for (int i = 0; i < files.size(); i++) {
            if (files.get(i).id.equals(node.id)) {
                files.remove(i);
                notifyItemRemoved(i);
                int newPos = findInsertPosition(node);
                files.add(newPos, node);
                notifyItemInserted(newPos);
                return;
            }
        }
    }

    void clear() {
        int size = files.size();
        if (size == 0) return;
        files.clear();
        notifyItemRangeRemoved(0, size);
    }

    /* ================= Recycler Overrides ================= */

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_file_item, parent, false);
        return new FileViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        holder.bind(files.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    /* ================= Helpers ================= */

    private int findInsertPosition(FileNode node) {
        for (int i = 0; i < files.size(); i++) {
            FileNode current = files.get(i);
            if (node.isFolder && !current.isFolder) return i;
            if (!node.isFolder && current.isFolder) continue;
            int compare = node.name.compareToIgnoreCase(current.name);
            if (compare < 0) return i;
        }
        return files.size();
    }

    /* ================= DiffUtil ================= */

    private static class DiffCallback extends DiffUtil.Callback {

        private final List<FileNode> oldList;
        private final List<FileNode> newList;

        DiffCallback(List<FileNode> oldList, List<FileNode> newList) {
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
        public boolean areItemsTheSame(int oldPos, int newPos) {
            return oldList.get(oldPos).id.equals(newList.get(newPos).id);
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            return oldList.get(oldPos).equals(newList.get(newPos));
        }
    }
}
