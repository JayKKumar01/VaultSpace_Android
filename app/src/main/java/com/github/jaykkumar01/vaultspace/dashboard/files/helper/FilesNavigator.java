package com.github.jaykkumar01.vaultspace.dashboard.files.helper;

import java.util.ArrayDeque;
import java.util.Deque;

public final class FilesNavigator {

    public interface Listener {
        void onFolderChanged(String folderId);
        void onPathChanged(Deque<NavEntry> path);
    }

    public static final class NavEntry {
        public final String id, name;
        public NavEntry(String id, String name) { this.id = id; this.name = name; }
    }

    private final Deque<NavEntry> stack = new ArrayDeque<>();
    private Listener listener;

    public void setListener(Listener l) { this.listener = l; }

    /* ================= Init ================= */

    public void init(String rootId, String rootName) {
        if (rootId == null || rootId.isEmpty()) return;
        stack.clear();
        stack.push(new NavEntry(rootId, rootName != null ? rootName : ""));
        dispatch();
    }

    /* ================= Navigation ================= */

    public void openFolder(String id, String name) {
        if (id == null || id.isEmpty()) return;
        stack.push(new NavEntry(id, name != null ? name : ""));
        dispatch();
    }

    public boolean goBack() {
        if (stack.size() <= 1) return false;
        stack.pop();
        dispatch();
        return true;
    }

    public void jumpTo(int indexFromTop) {
        if (indexFromTop < 0 || indexFromTop >= stack.size()) return;
        while (stack.size() > indexFromTop + 1) stack.pop();
        dispatch();
    }

    /* ================= State ================= */

    public String getCurrentFolderId() {
        NavEntry top = stack.peek();
        return top != null ? top.id : null;
    }

    public String getCurrentFolderName() {
        NavEntry top = stack.peek();
        return top != null ? top.name : null;
    }

    public boolean isAtRoot() { return stack.size() <= 1; }

    public Deque<NavEntry> getPath() { return new ArrayDeque<>(stack); }

    /* ================= Internal ================= */

    private void dispatch() {
        if (listener == null) return;

        NavEntry top = stack.peek();
        if (top == null) return;

        listener.onFolderChanged(top.id);
        listener.onPathChanged(new ArrayDeque<>(stack));
    }
}