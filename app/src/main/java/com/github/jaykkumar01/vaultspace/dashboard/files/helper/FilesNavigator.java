package com.github.jaykkumar01.vaultspace.dashboard.files.helper;

import com.github.jaykkumar01.vaultspace.models.FileNode;
import java.util.ArrayDeque;
import java.util.Deque;

public final class FilesNavigator {

    private final Deque<String> navStack = new ArrayDeque<>();

    /* ================= Root ================= */

    public void onRootReady(String rootId) {
        navStack.clear();
        navStack.push(rootId);
    }

    /* ================= Navigation ================= */

    public String openFolder(FileNode node) {
        if (node == null || !node.isFolder) return null;
        navStack.push(node.id);
        return node.id;
    }

    public String navigateBack() {
        if (navStack.size() <= 1) return null;
        navStack.pop();
        return navStack.peek();
    }

    public boolean canNavigateBack() { return navStack.size() > 1; }

    public String getCurrentFolderId() { return navStack.peek(); }

    public String getHeaderText() { return canNavigateBack() ? "← Back" : "Files Vault"; }
}