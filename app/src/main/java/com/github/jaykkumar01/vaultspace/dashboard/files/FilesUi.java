package com.github.jaykkumar01.vaultspace.dashboard.files;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.dashboard.base.BaseSectionUi;
import com.github.jaykkumar01.vaultspace.models.FileNode;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalHost;
import com.github.jaykkumar01.vaultspace.views.popups.form.FormSpec;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class FilesUi extends BaseSectionUi implements
        FilesRepository.Listener,
        FilesContentView.OnItemInteractionListener,
        FilesContentView.OnFilesActionListener {

    private static final String TAG = "VaultSpace:FilesUI";
    private final ModalHost modalHost;

    /* ================= State ================= */

    private enum UiState { UNINITIALIZED, LOADING, EMPTY, CONTENT, ERROR }

    /* ================= Core ================= */

    private final FilesRepository repo;
    private final Deque<String> navStack = new ArrayDeque<>();

    private UiState state = UiState.UNINITIALIZED;
    private boolean released;

    private FilesContentView content;

    /* ================= Constructor ================= */

    public FilesUi(Context context, FrameLayout container, ModalHost modalHost) {
        super(context, container);
        this.modalHost = modalHost;
        repo = FilesRepository.getInstance(context);
        setupStaticUi();
    }

    private void setupStaticUi() {
        loadingView.setText("Loading files…");

        emptyView.setIcon(R.drawable.ic_files_empty);
        emptyView.setTitle("No files found");
        emptyView.setSubtitle("Files reflect how your data is stored in VaultSpace.");
        emptyView.setPrimaryAction("Upload Files", v -> onUploadClick());
        emptyView.setSecondaryAction("Create Folder", v -> onCreateFolderClick());

        loadingView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        contentView.setVisibility(View.GONE);
    }

    @Override
    protected View createContentView(Context context) {
        content = new FilesContentView(context, this, this);
        return content;
    }

    /* ================= Lifecycle ================= */

    @Override
    public void show() {
        if (released || state != UiState.UNINITIALIZED) return;

        repo.addListener(this);
        moveToState(UiState.LOADING);
        repo.initialize();
    }

    @Override
    public void onRelease() {
        released = true;
        repo.removeListener(this);
    }

    @Override
    public boolean handleBack() {
        return navigateBack();
    }

    /* ================= Navigation ================= */

    private void openFolder(FileNode node) {
        if (node == null || !node.isFolder) return;

        navStack.push(node.id);
        updateHeader();
        repo.openFolder(node.id);
    }

    private boolean navigateBack() {
        if (navStack.size() <= 1) return false;

        navStack.pop();
        updateHeader();

        String parentId = navStack.peek();
        if (parentId != null) repo.openFolder(parentId);
        return true;
    }

    private void updateHeader() {
        if (content == null) return;
        content.setHeaderText(navStack.size() <= 1 ? "Files Vault" : "← Back");
    }

    /* ================= Repository Callbacks ================= */

    @Override
    public void onReady(String rootId) {
        navStack.clear();
        navStack.push(rootId);
        updateHeader();
        repo.openFolder(rootId);
    }

    @Override
    public void onFolderChanged(String folderId, List<FileNode> children) {
        boolean isRoot = folderId.equals(repo.getRootId());
        boolean isEmpty = children == null || children.isEmpty();

        if (isRoot && isEmpty) {
            moveToState(UiState.EMPTY);
            return;
        }

        content.submitList(children);
        moveToState(UiState.CONTENT);
    }

    @Override
    public void onFileAdded(String parentId, FileNode node) {
        content.addFile(node);
        if (state != UiState.CONTENT) moveToState(UiState.CONTENT);
    }

    @Override
    public void onFileRemoved(String parentId, String nodeId) {
        content.removeFile(nodeId); // need to check if empty now
    }

    @Override
    public void onFileUpdated(String parentId, FileNode node) {
        content.updateFile(node);
    }

    @Override
    public void onError(Exception e) {
        Log.e(TAG, "Repository error", e);
        moveToState(UiState.ERROR);
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
        navigateBack();
    }

    @Override
    public void onCreateFolderClick() {
        Log.d(TAG, "Create folder clicked");

        modalHost.request(new FormSpec(
                "Create Folder",
                "Folder name",
                "Create",
                name -> {
                    String parentId = getCurrentFolderId();
                    repo.createFolder(parentId,name);
                },
                null
        ));
    }

    @Override
    public void onUploadClick() {
        Log.d(TAG, "Upload clicked");
    }

    /* ================= Helpers ================= */

    private String getCurrentFolderId(){
        return navStack.peek();
    }

    /* ================= State Handling ================= */

    private void moveToState(UiState newState) {
        if (state == newState) return;
        state = newState;

        switch (newState) {
            case LOADING -> showLoading();
            case CONTENT -> showContent();
            case EMPTY, ERROR -> showEmpty();
        }
    }
}