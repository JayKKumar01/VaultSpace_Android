package com.github.jaykkumar01.vaultspace.dashboard.files.helper;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.github.jaykkumar01.vaultspace.dashboard.files.FilesRepository;
import com.github.jaykkumar01.vaultspace.models.FileNode;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalHost;
import com.github.jaykkumar01.vaultspace.views.popups.form.FormSpec;
import com.github.jaykkumar01.vaultspace.views.popups.list.ListSpec;

import java.util.List;
import java.util.function.Supplier;

public final class FileActionController {

    private static final String TAG = "VaultSpace:FileActions";

    /* ================= Dependencies ================= */

    private final Context context;
    private final FilesRepository repo;
    private final ModalHost modalHost;
    private final Supplier<String> currentFolderProvider;

    /* ================= Constructor ================= */

    public FileActionController(Context context,
                                FilesRepository repo,
                                ModalHost modalHost,
                                Supplier<String> currentFolderProvider) {
        this.context = context;
        this.repo = repo;
        this.modalHost = modalHost;
        this.currentFolderProvider = currentFolderProvider;
    }

    /* ================= Public API ================= */

    public void onFileClick(FileNode node) {
        if (node == null) return;
        modalHost.request(buildDownloadSpec(node));
    }

    public void onNodeLongClick(FileNode node) {
        if (node == null) return;
        modalHost.request(buildLongClickSpec(node));
    }

    public void onCreateFolderClick() {
        modalHost.request(new FormSpec(
                "Create Folder",
                "Folder name",
                "Create",
                name -> repo.createFolder(currentFolderProvider.get(), name),
                null
        ));
    }

    /* ================= Spec Builders ================= */

    private ListSpec buildDownloadSpec(FileNode node) {
        return new ListSpec(
                "File Options",
                List.of("Download"),
                index -> performDownload(node),
                null
        );
    }

    private ListSpec buildLongClickSpec(FileNode node) {
        return new ListSpec(
                "Actions",
                List.of("Rename", "Delete"),
                index -> {
                    switch (index) {
                        case 0 -> showRenameDialog(node);
                        case 1 -> performDelete(node);
                    }
                },
                null
        );
    }

    /* ================= Actions ================= */

    private void performDownload(FileNode node) {
        Log.d(TAG, "Download: " + node.name);
        Toast.makeText(context, "Downloading " + node.name, Toast.LENGTH_SHORT).show();
        // repo.download(node.id);
    }

    private void performDelete(FileNode node) {
        Log.d(TAG, "Delete: " + node.name);
        repo.deleteNode(node.id);
    }

    private void showRenameDialog(FileNode node) {
        modalHost.request(new FormSpec(
                "Rename",
                "New name",
                "Rename",
                name -> repo.renameNode(node.id, name),
                null
        ));
    }
}