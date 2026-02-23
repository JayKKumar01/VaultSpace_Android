package com.github.jaykkumar01.vaultspace.dashboard.files.helper;

import com.github.jaykkumar01.vaultspace.dashboard.files.FilesRepository;
import com.github.jaykkumar01.vaultspace.models.FileNode;
import java.util.List;

public final class FilesCoordinator implements FilesRepository.Listener {

    public interface Renderer {
        void renderLoading();
        void renderEmpty();
        void renderContent(List<FileNode> nodes);
        void renderError();
        void updateHeader(String text);
    }

    private final FilesRepository repo;
    private final FilesNavigator navigator;
    private final Renderer renderer;

    public FilesCoordinator(FilesRepository repo, FilesNavigator navigator, Renderer renderer) {
        this.repo = repo;
        this.navigator = navigator;
        this.renderer = renderer;
    }

    /* ================= Lifecycle ================= */

    public void start() {
        repo.addListener(this);
        renderer.renderLoading();
        repo.initialize();
    }

    public void stop() { repo.removeListener(this); }

    /* ================= User Intents ================= */

    public void onFolderSelected(FileNode node) {
        String id = navigator.openFolder(node);
        if (id == null) return;
        renderer.updateHeader(navigator.getHeaderText());
        repo.openFolder(id);
    }

    public boolean onBackRequested() {
        if (!navigator.canNavigateBack()) return false;
        String id = navigator.navigateBack();
        if (id == null) return false;
        renderer.updateHeader(navigator.getHeaderText());
        repo.openFolder(id);
        return true;
    }

    /* ================= Repo Callbacks ================= */

    @Override
    public void onReady(String rootId) {
        navigator.onRootReady(rootId);
        renderer.updateHeader(navigator.getHeaderText());
        repo.openFolder(rootId);
    }

    @Override
    public void onFolderChanged(String folderId, List<FileNode> children) {
        boolean empty = children == null || children.isEmpty();
        if (empty && !navigator.canNavigateBack()) {
            renderer.renderEmpty();
            return;
        }
        renderer.renderContent(children);
    }

    @Override public void onFileAdded(String parentId, FileNode node) {}
    @Override public void onFileRemoved(String parentId, String nodeId) {}
    @Override public void onFileUpdated(String parentId, FileNode node) {}
    @Override public void onError(Exception e) { renderer.renderError(); }
}