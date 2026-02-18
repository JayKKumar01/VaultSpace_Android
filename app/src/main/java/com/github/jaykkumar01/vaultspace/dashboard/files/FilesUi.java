package com.github.jaykkumar01.vaultspace.dashboard.files;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.dashboard.base.BaseSectionUi;
import com.github.jaykkumar01.vaultspace.models.FileNode;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalHost;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class FilesUi extends BaseSectionUi implements FilesRepository.Listener, FilesContentView.OnItemInteractionListener{

    private static final String TAG = "VaultSpace:FilesUI";

    private final FilesRepository repo;
    private final Deque<String> navStack = new ArrayDeque<>();
    private FilesContentView content;

    public FilesUi(Context context, FrameLayout container, ModalHost hostView) {
        super(context, container, hostView);
        repo = FilesRepository.getInstance(context);
        setupStaticUi();
    }

    private void setupStaticUi() {
        loadingView.setText("Loading files…");
        emptyView.setIcon(R.drawable.ic_files_empty);
        emptyView.setTitle("No files found");
        emptyView.setSubtitle("Files reflect how your data is stored in VaultSpace.");
        emptyView.setPrimaryAction("Upload Files", v -> Log.d(TAG, "Upload stub"));
        emptyView.setSecondaryAction("Create Folder", v -> Log.d(TAG, "Create folder stub"));
    }

    @Override
    protected View createContentView(Context context) {
        content = new FilesContentView(context,this);
        return content;
    }

    /* ================= Lifecycle ================= */

    @Override
    public void show() {
        Log.d(TAG, "UI show()");
        repo.addListener(this);
        showLoading();
        repo.initialize();
    }

    @Override
    public void onRelease() {
        Log.d(TAG, "UI release()");
        repo.removeListener(this);
    }

    /* ================= Navigation ================= */

    private void openFolder(FileNode node) {
        if (!node.isFolder) return;
        navStack.push(node.id);
        Log.d(TAG, "Open folder → stack size: " + navStack.size());
        repo.openFolder(node.id);
    }

    @Override
    public boolean handleBack() {
        if (navStack.size() > 1) {
            navStack.pop();
            Log.d(TAG, "Back → stack size: " + navStack.size());
            repo.openFolder(navStack.peek());
            return true;
        }
        return false;
    }

    /* ================= Repository Callbacks ================= */

    @Override
    public void onReady(String rootId) {
        Log.d(TAG, "Repository ready. Root: " + rootId);
        navStack.clear();
        navStack.push(rootId);
    }

    @Override
    public void onFolderChanged(String folderId, List<FileNode> children) {
        Log.d(TAG, "Folder changed → id: " + folderId + " | children: " + children.size());

        if (children.isEmpty()) {
            showEmpty();
        } else {
            content.submitList(children);
            showContent();
        }
    }

    @Override
    public void onError(Exception e) {
        Log.e(TAG, "Repository error", e);
    }

    @Override
    public void onFileClick(FileNode node) {
        Log.d(TAG, "File click: " + node.name + " (" + node.id + ")");
    }

    @Override
    public void onFolderClick(FileNode node) {
        Log.d(TAG, "Folder click: " + node.name + " (" + node.id + ")");
        openFolder(node);
    }

    @Override
    public void onFileLongClick(FileNode node) {
        Log.d(TAG, "File long click: " + node.name);
    }

    @Override
    public void onFolderLongClick(FileNode node) {
        Log.d(TAG, "Folder long click: " + node.name);
    }
}
