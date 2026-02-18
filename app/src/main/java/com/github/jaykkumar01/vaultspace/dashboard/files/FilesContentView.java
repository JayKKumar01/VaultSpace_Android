package com.github.jaykkumar01.vaultspace.dashboard.files;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.jaykkumar01.vaultspace.models.FileNode;

import java.util.List;

public class FilesContentView extends RecyclerView {

    public interface OnItemInteractionListener {
        void onFileClick(FileNode node);

        void onFolderClick(FileNode node);

        void onFileLongClick(FileNode node);

        void onFolderLongClick(FileNode node);
    }

    private final FilesAdapter adapter;

    public FilesContentView(@NonNull Context context, @NonNull OnItemInteractionListener listener) {
        super(context);
        setLayoutManager(new LinearLayoutManager(context));
        adapter = new FilesAdapter(listener);
        setAdapter(adapter);
    }

    public void submitList(List<FileNode> files) {
        adapter.submitList(files);
    }

    public void addFile(FileNode node) {
        adapter.addFile(node);
    }

    public void removeFile(String id) {
        adapter.removeFile(id);
    }

    public void updateFile(FileNode node) {
        adapter.updateFile(node);
    }

    public void clear() {
        adapter.clear();
    }
    private static class VerticalSpaceDecoration extends RecyclerView.ItemDecoration {

        private final int spacePx;

        VerticalSpaceDecoration(int spacePx) {
            this.spacePx = spacePx;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect,
                                   @NonNull View view,
                                   @NonNull RecyclerView parent,
                                   @NonNull RecyclerView.State state) {

            int position = parent.getChildAdapterPosition(view);
            if (position > 0) outRect.top = spacePx;
        }
    }

}
