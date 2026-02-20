package com.github.jaykkumar01.vaultspace.core.session.cache;

import com.github.jaykkumar01.vaultspace.models.FileNode;

import java.util.*;

public final class FilesCache extends VaultCache {

    private final Map<String, List<FileNode>> childrenMap = new HashMap<>();
    private final Map<String, String> parentMap = new HashMap<>();
    private final Set<String> loadedFolders = new HashSet<>();

    /* ================= Root ================= */

    public void initialize(String rootId) {
        childrenMap.putIfAbsent(rootId, new ArrayList<>());
        loadedFolders.add(rootId);
        markInitialized();
    }

    /* ================= Folder State ================= */

    public boolean isFolderLoaded(String folderId) {
        return loadedFolders.contains(folderId);
    }

    public void replaceChildren(String parentId, List<FileNode> children) {
        childrenMap.put(parentId, new ArrayList<>(children));
        loadedFolders.add(parentId);
        for (FileNode n : children) {
            parentMap.put(n.id, parentId);
            if (n.isFolder) childrenMap.putIfAbsent(n.id, new ArrayList<>());
        }
        sort(childrenMap.get(parentId));
    }

    public List<FileNode> getChildren(String parentId) {
        List<FileNode> list = childrenMap.get(parentId);
        return list == null ? Collections.emptyList() : new ArrayList<>(list);
    }

    /* ================= Mutations ================= */

    public void addNode(String parentId, FileNode node) {
        parentMap.put(node.id, parentId);
        childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(node);
        if (node.isFolder) childrenMap.putIfAbsent(node.id, new ArrayList<>());
        sort(childrenMap.get(parentId));
    }
    /* ================= Mutations ================= */

    public void replaceNode(String id, FileNode updated) {
        String parent = parentMap.get(id);
        if (parent == null) return;

        List<FileNode> siblings = childrenMap.get(parent);
        if (siblings == null) return;

        for (int i = 0; i < siblings.size(); i++) {
            if (siblings.get(i).id.equals(id)) {
                siblings.set(i, updated);
                break;
            }
        }

        parentMap.put(updated.id, parent);
        sort(siblings);
    }

    public void deleteNode(String id) {
        if (!parentMap.containsKey(id)) return;
        if (childrenMap.containsKey(id)) {
            List<FileNode> children = new ArrayList<>(childrenMap.get(id));
            for (FileNode c : children) deleteNode(c.id);
            childrenMap.remove(id);
            loadedFolders.remove(id);
        }
        String parent = parentMap.get(id);
        if (parent != null) {
            List<FileNode> siblings = childrenMap.get(parent);
            if (siblings != null) siblings.removeIf(n -> n.id.equals(id));
        }
        parentMap.remove(id);
    }

    public String getParent(String id) {
        return parentMap.get(id);
    }

    private void sort(List<FileNode> list) {
        list.sort((a, b) -> {
            if (a.isFolder != b.isFolder) return a.isFolder ? -1 : 1;
            return a.name.compareToIgnoreCase(b.name);
        });
    }

    @Override
    protected void onClear() {
        childrenMap.clear();
        parentMap.clear();
        loadedFolders.clear();
    }
}
