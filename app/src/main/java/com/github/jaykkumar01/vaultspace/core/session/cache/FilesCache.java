package com.github.jaykkumar01.vaultspace.core.session.cache;

import com.github.jaykkumar01.vaultspace.models.FileNode;

import java.util.*;

public final class FilesCache extends VaultCache {

    /* ================= Internal State ================= */

    private final Map<String, List<FileNode>> childrenMap = new HashMap<>();
    private final Map<String, String> parentMap = new HashMap<>();
    private final Set<String> loadedFolders = new HashSet<>();

    /* ================= Root ================= */

    public void initialize(String rootId) {
        if (rootId == null) return;
        childrenMap.putIfAbsent(rootId, new ArrayList<>());
        loadedFolders.add(rootId);
        markInitialized();
    }

    /* ================= Basic Access ================= */

    public boolean isFolderLoaded(String folderId) {
        return folderId != null && loadedFolders.contains(folderId);
    }

    public List<FileNode> getChildren(String parentId) {
        if (parentId == null) return Collections.emptyList();
        List<FileNode> list = childrenMap.get(parentId);
        return list == null ? Collections.emptyList() : new ArrayList<>(list);
    }

    public String getParent(String id) {
        return id == null ? null : parentMap.get(id);
    }

    /* ================= Sorted Insert ================= */

    private int compare(FileNode a, FileNode b) {
        if (a.isFolder != b.isFolder) return a.isFolder ? -1 : 1;
        return a.name.compareToIgnoreCase(b.name);
    }

    private void insertSorted(List<FileNode> list, FileNode node) {
        int low = 0, high = list.size();
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (compare(node, list.get(mid)) > 0) low = mid + 1;
            else high = mid;
        }
        list.add(low, node);
    }

    /* ================= Mutations ================= */

    public void addNode(String parentId, FileNode node) {
        if (parentId == null || node == null) return;

        parentMap.put(node.id, parentId);

        List<FileNode> siblings = childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>());
        insertSorted(siblings, node);

        if (node.isFolder) childrenMap.putIfAbsent(node.id, new ArrayList<>());
    }

    public void replaceNode(String id, FileNode updated) {
        if (id == null || updated == null) return;

        String parent = parentMap.get(id);
        if (parent == null) return;

        List<FileNode> siblings = childrenMap.get(parent);
        if (siblings == null) return;

        int index = indexOf(siblings, id);
        if (index == -1) return;

        siblings.remove(index);
        insertSorted(siblings, updated);

        parentMap.put(updated.id, parent);
    }

    public void replaceChildren(String parentId, List<FileNode> children) {
        if (parentId == null) return;

        List<FileNode> newList = new ArrayList<>();
        if (children != null) {
            for (FileNode node : children) {
                if (node == null) continue;
                insertSorted(newList, node);
                parentMap.put(node.id, parentId);
                if (node.isFolder) childrenMap.putIfAbsent(node.id, new ArrayList<>());
            }
        }

        childrenMap.put(parentId, newList);
        loadedFolders.add(parentId);
    }

    /* ================= Optimized Delete ================= */

    public void deleteNode(String rootId) {
        if (rootId == null) return;

        String parent = parentMap.get(rootId);
        if (parent == null) return;

        // Collect entire subtree IDs
        Set<String> subtreeIds = new HashSet<>();
        collectSubtreeIds(rootId, subtreeIds);

        // Remove root from parent once
        List<FileNode> siblings = childrenMap.get(parent);
        if (siblings != null) {
            int index = indexOf(siblings, rootId);
            if (index != -1) siblings.remove(index);
        }

        // Remove all subtree entries in O(S)
        for (String id : subtreeIds) {
            childrenMap.remove(id);
            parentMap.remove(id);
            loadedFolders.remove(id);
        }
    }

    private void collectSubtreeIds(String id, Set<String> ids) {
        if (id == null || ids.contains(id)) return;

        ids.add(id);

        List<FileNode> children = childrenMap.get(id);
        if (children == null) return;

        for (FileNode child : children) {
            if (child != null) collectSubtreeIds(child.id, ids);
        }
    }

    private int indexOf(List<FileNode> list, String id) {
        for (int i = 0; i < list.size(); i++) {
            FileNode n = list.get(i);
            if (n != null && id.equals(n.id)) return i;
        }
        return -1;
    }

    /* ================= Subtree Snapshot ================= */

    public Subtree exportSubtree(String rootId) {
        if (rootId == null) return null;

        String parentId = parentMap.get(rootId);
        if (parentId == null) return null;

        List<FileNode> siblings = childrenMap.get(parentId);
        if (siblings == null) return null;

        FileNode rootNode = null;
        for (FileNode n : siblings) {
            if (n != null && rootId.equals(n.id)) {
                rootNode = n;
                break;
            }
        }
        if (rootNode == null) return null;

        Map<String, List<FileNode>> childrenCopy = new HashMap<>();
        Map<String, String> parentCopy = new HashMap<>();
        Set<String> loadedCopy = new HashSet<>();

        captureRecursive(rootId, childrenCopy, parentCopy, loadedCopy);

        return new Subtree(rootNode, parentId, childrenCopy, parentCopy, loadedCopy);
    }

    public void importSubtree(Subtree subtree) {
        if (subtree == null) return;

        List<FileNode> siblings = childrenMap.computeIfAbsent(subtree.parentId, k -> new ArrayList<>());
        insertSorted(siblings, subtree.rootNode);

        parentMap.put(subtree.rootNode.id, subtree.parentId);

        for (Map.Entry<String, List<FileNode>> e : subtree.children.entrySet()) {
            childrenMap.put(e.getKey(), new ArrayList<>(e.getValue()));
        }

        parentMap.putAll(subtree.parents);
        loadedFolders.addAll(subtree.loaded);
    }

    private void captureRecursive(String id,
                                  Map<String, List<FileNode>> childrenCopy,
                                  Map<String, String> parentCopy,
                                  Set<String> loadedCopy) {

        List<FileNode> children = childrenMap.get(id);
        if (children == null) return;

        childrenCopy.put(id, new ArrayList<>(children));
        loadedCopy.add(id);

        for (FileNode child : children) {
            if (child == null) continue;
            parentCopy.put(child.id, id);
            if (child.isFolder) captureRecursive(child.id, childrenCopy, parentCopy, loadedCopy);
        }
    }

    /* ================= Clear ================= */

    @Override
    protected void onClear() {
        childrenMap.clear();
        parentMap.clear();
        loadedFolders.clear();
    }

    /* ================= Subtree Model ================= */

    public record Subtree(FileNode rootNode,
                          String parentId,
                          Map<String, List<FileNode>> children,
                          Map<String, String> parents,
                          Set<String> loaded) {
    }
}