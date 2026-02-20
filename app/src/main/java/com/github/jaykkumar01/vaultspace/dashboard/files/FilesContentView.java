package com.github.jaykkumar01.vaultspace.dashboard.files;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.models.FileNode;

import java.util.List;

public final class FilesContentView extends FrameLayout {

    /* ================= Listeners ================= */

    public interface OnItemInteractionListener {
        void onFileClick(FileNode node);
        void onFolderClick(FileNode node);
        void onFileLongClick(FileNode node);
        void onFolderLongClick(FileNode node);
    }

    public interface OnFilesActionListener {
        void onBackClick();
        void onCreateFolderClick();
        void onUploadClick();
    }

    /* ================= Core ================= */

    private final FilesAdapter adapter;
    private final RecyclerView recyclerView;
    private final View emptyContainer;
    private final TextView backText;

    /* ================= Constructor ================= */

    public FilesContentView(@NonNull Context context,
                            @NonNull OnItemInteractionListener itemListener,
                            @NonNull OnFilesActionListener actionListener) {
        super(context);

        View root = LayoutInflater.from(context)
                .inflate(R.layout.view_files_content, this, true);

        backText = root.findViewById(R.id.back_text);
        recyclerView = root.findViewById(R.id.recycler_view);
        emptyContainer = root.findViewById(R.id.empty_container);

        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        adapter = new FilesAdapter(itemListener);
        recyclerView.setAdapter(adapter);

        /* Header Actions */

        backText.setOnClickListener(v -> actionListener.onBackClick());

        root.findViewById(R.id.btn_create)
                .setOnClickListener(v -> actionListener.onCreateFolderClick());

        root.findViewById(R.id.btn_upload)
                .setOnClickListener(v -> actionListener.onUploadClick());
    }

    /* ================= Empty Handling ================= */

    private void updateEmptyVisibility() {
        boolean isEmpty = adapter.getItemCount() == 0;

        recyclerView.setVisibility(isEmpty ? GONE : VISIBLE);
        emptyContainer.setVisibility(isEmpty ? VISIBLE : GONE);
    }

    /* ================= Public API ================= */

    public void submitList(List<FileNode> files) {
        adapter.submitList(files);
        updateEmptyVisibility();
    }

    public void addFile(FileNode node) {
        adapter.addFile(node);
        updateEmptyVisibility();
    }

    public void removeFile(String id) {
        adapter.removeFile(id);
        updateEmptyVisibility();
    }

    public void updateFile(FileNode node) {
        adapter.updateFile(node);
    }

    public void clear() {
        adapter.clear();
        updateEmptyVisibility();
    }

    public void setHeaderText(String text) {
        backText.setText(text);
    }
}