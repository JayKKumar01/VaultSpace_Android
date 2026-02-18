package com.github.jaykkumar01.vaultspace.dashboard.files;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.models.FileNode;
import java.util.List;

public class FilesContentView extends FrameLayout {

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
    private final TextView backText;   // reference to back text

    public FilesContentView(@NonNull Context context,
                            @NonNull OnItemInteractionListener itemListener,
                            @NonNull OnFilesActionListener actionListener) {
        super(context);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        addView(root, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

        /* ===== Inflate Header ===== */

        View headerView = LayoutInflater.from(context)
                .inflate(R.layout.view_files_header, root, false);

        root.addView(headerView);

        backText = headerView.findViewById(R.id.back_text);

        backText.setOnClickListener(v -> actionListener.onBackClick());

        headerView.findViewById(R.id.btn_create)
                .setOnClickListener(v -> actionListener.onCreateFolderClick());

        headerView.findViewById(R.id.btn_upload)
                .setOnClickListener(v -> actionListener.onUploadClick());

        /* ===== RecyclerView ===== */

        RecyclerView recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        adapter = new FilesAdapter(itemListener);
        recyclerView.setAdapter(adapter);

        LinearLayout.LayoutParams rvParams =
                new LinearLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        0);
        rvParams.weight = 1f;

        root.addView(recyclerView, rvParams);
    }

    /* ================= Back Visibility ================= */

    public void setHeaderText(String text) {
        backText.setText(text);
    }


    /* ================= Public API ================= */

    public void submitList(List<FileNode> files) { adapter.submitList(files); }
    public void addFile(FileNode node) { adapter.addFile(node); }
    public void removeFile(String id) { adapter.removeFile(id); }
    public void updateFile(FileNode node) { adapter.updateFile(node); }
    public void clear() { adapter.clear(); }
}
