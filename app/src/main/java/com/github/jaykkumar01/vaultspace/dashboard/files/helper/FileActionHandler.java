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

public final class FileActionHandler {

    private static final String TAG = "VaultSpace:FileActions";

    private final Context context;
    private final FilesRepository repo;
    private final ModalHost modalHost;
    private final Supplier<String> currentFolderProvider;

    public FileActionHandler(Context context, FilesRepository repo, ModalHost modalHost, Supplier<String> currentFolderProvider) {
        this.context = context;
        this.repo = repo;
        this.modalHost = modalHost;
        this.currentFolderProvider = currentFolderProvider;
    }

    /* ================= Public API ================= */

    public void onFileClick(FileNode node) {
        if (node == null) return;
        modalHost.request(new ListSpec("File Options", List.of("Download"),
                i -> performDownload(node), null));
    }

    public void onNodeLongClick(FileNode node) {
        if (node == null) return;
        modalHost.request(new ListSpec("Actions", List.of("Rename","Delete"),
                i -> { if (i==0) showRename(node); else performDelete(node); }, null));
    }

    public void onCreateFolderClick() {
        modalHost.request(new FormSpec("Create Folder","Folder name","Create",
                name -> repo.createFolder(currentFolderProvider.get(), name), null));
    }

    /* ================= Actions ================= */

    private void performDownload(FileNode node) {
        Log.d(TAG,"Download: "+node.name);
        Toast.makeText(context,"Downloading "+node.name,Toast.LENGTH_SHORT).show();
        // repo.download(node.id);
    }

    private void performDelete(FileNode node) {
        Log.d(TAG,"Delete: "+node.name);
        repo.deleteNode(node.id);
    }

    private void showRename(FileNode node) {
        modalHost.request(new FormSpec("Rename","New name","Rename",
                name -> repo.renameNode(node.id,name), null));
    }
}