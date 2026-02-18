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

public class FilesUi extends BaseSectionUi implements
        FilesRepository.Listener,
        FilesContentView.OnItemInteractionListener,
        FilesContentView.OnFilesActionListener {

    private static final String TAG = "VaultSpace:FilesUI";

    /* ================= Core ================= */

    private final FilesRepository repo;
    private final Deque<String> navStack = new ArrayDeque<>();
    private FilesContentView content;

    /* ================= Constructor ================= */

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
        content = new FilesContentView(context, this, this);
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
        repo.removeListener(this);
    }

    /* ================= Navigation ================= */

    private void openFolder(FileNode node) {
        if (node == null || !node.isFolder) return;

        navStack.push(node.id);
        updateHeader();

        Log.d(TAG, "Open folder → depth: " + navStack.size());
        repo.openFolder(node.id);
    }

    private boolean navigateBack() {
        if (navStack.size() <= 1) return false;

        navStack.pop();
        updateHeader();

        String parentId = navStack.peek();
        if (parentId != null) {
            Log.d(TAG, "Navigate back → depth: " + navStack.size());
            repo.openFolder(parentId);
        }

        return true;
    }

    private void updateHeader() {
        if (content == null) return;

        if (navStack.size() <= 1) {
            content.setHeaderText("Files Vault");
        } else {
            content.setHeaderText("← Back");
        }
    }

    @Override
    public boolean handleBack() {
        return navigateBack();
    }

    /* ================= Repository Callbacks ================= */

    @Override
    public void onReady(String rootId) {
        Log.d(TAG, "Repository ready");

        navStack.clear();
        navStack.push(rootId);

        updateHeader();
        repo.openFolder(rootId);
    }

    @Override
    public void onFolderChanged(String folderId, List<FileNode> children) {
        if (children == null || children.isEmpty()) {
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

    /* ================= Item Interactions ================= */

    @Override
    public void onFileClick(FileNode node) {
        Log.d(TAG, "File click: " + node.name);
    }

    @Override
    public void onFolderClick(FileNode node) {
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

    /* ================= Header Actions ================= */

    @Override
    public void onBackClick() {
        navigateBack(); // safe at root (no-op)
    }

    @Override
    public void onCreateFolderClick() {
        Log.d(TAG, "Create folder clicked");
    }

    @Override
    public void onUploadClick() {
        Log.d(TAG, "Upload clicked");
    }
}
