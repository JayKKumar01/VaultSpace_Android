package com.github.jaykkumar01.vaultspace.dashboard.files;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.core.session.cache.FilesCache;
import com.github.jaykkumar01.vaultspace.dashboard.files.drive.FilesDriveHelper;
import com.github.jaykkumar01.vaultspace.models.FileNode;
import com.google.api.services.drive.Drive;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class FilesRepository {

    /* ================= Singleton ================= */

    private static volatile FilesRepository INSTANCE;

    public static FilesRepository getInstance(Context c) {
        if (INSTANCE == null) synchronized (FilesRepository.class) {
            if (INSTANCE == null) INSTANCE = new FilesRepository(c.getApplicationContext());
        }
        return INSTANCE;
    }

    /* ================= Listener ================= */

    public interface Listener {
        void onReady(String rootId);

        void onFolderChanged(String folderId, List<FileNode> children);

        void onFileAdded(String parentId, FileNode node);

        void onFileRemoved(String parentId, String nodeId);

        void onFileUpdated(String parentId, FileNode node);

        void onError(Exception e);
    }

    /* ================= Core ================= */

    private static final String FOLDER_MIME = "application/vnd.google-apps.folder";

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Set<Listener> listeners = new HashSet<>();
    private final FilesDriveHelper driveHelper;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final FilesCache cache;

    private String rootId;

    private FilesRepository(Context c) {
        cache = new UserSession(c).getVaultCache().files;
        driveHelper = new FilesDriveHelper(c);
    }

    /* ================= Initialize ================= */

    public void initialize() {
        executor.execute(() -> {
            try {
                rootId = driveHelper.resolveFilesRoot();

                if (!cache.isInitialized()) cache.initialize(rootId);
                notifyReady();

                if (cache.getChildren(rootId).isEmpty()) {
                    loadFolderFromDrive(rootId);
                } else {
                    notifyFolder(rootId);
                }

            } catch (Exception e) {
                notifyError(e);
            }
        });
    }

    private void loadFolderFromDrive(String folderId) {
        driveHelper.fetchFolderChildren(
                executor,
                folderId,
                nodes -> {
                    cache.replaceChildren(folderId, nodes); // IMPORTANT
                    notifyFolder(folderId);
                },
                this::notifyError
        );
    }

    public String getRootId() {
        return rootId;
    }

    /* ================= Navigation ================= */

    public void openFolder(String folderId) {
        if (folderId == null) return;

        // 1️⃣ Always show cache instantly
        notifyFolder(folderId);

        // 2️⃣ If not loaded → fetch from Drive
        if (!cache.isFolderLoaded(folderId)) {
            loadFolderFromDrive(folderId);
        }
    }

    /* ================= Fake Mutations ================= */

    public void createFolder(String parentId, String name) {
        if (parentId == null || name == null || name.trim().isEmpty()) return;

        String trimmed = name.trim();

        // 🚫 Duplicate check (fast fail)
        List<FileNode> existing = cache.getChildren(parentId);
        for (FileNode n : existing) {
            if (n != null && trimmed.equalsIgnoreCase(n.name)) {
                notifyError(new RuntimeException("Folder already exists"));
                return;
            }
        }

        // 1️⃣ Temp node (optimistic UI)
        String tempId = "temp_" + UUID.randomUUID();
        FileNode tempNode = new FileNode(
                tempId,
                trimmed,
                FOLDER_MIME,
                0,
                System.currentTimeMillis()
        );

        cache.addNode(parentId, tempNode);
        notifyAdded(parentId, tempNode);

        // 2️⃣ Background Drive call
        executor.execute(() -> {
            try {
                FileNode realNode = driveHelper.createFolder(parentId, trimmed);

                // 3️⃣ Replace temp with real
                cache.deleteNode(tempId);
                cache.addNode(parentId, realNode);

                notifyRemoved(parentId, tempId);
                notifyAdded(parentId, realNode);

            } catch (Exception e) {

                // 4️⃣ Rollback
                String parentCheck = cache.getParent(tempId);
                if (parentCheck != null) {
                    cache.deleteNode(tempId);
                    notifyRemoved(parentCheck, tempId);
                }

                notifyError(e);
            }
        });
    }

    public void createFile(String parentId, String name, String mime, long size) {
        FileNode node = new FileNode(UUID.randomUUID().toString(), name, mime, size, System.currentTimeMillis());
        cache.addNode(parentId, node);
        notifyAdded(parentId, node);
    }

    public void renameNode(FileNode node, String newName) {
        if (node == null) return;

        String parentId = cache.getParent(node.id);
        if (parentId == null) return;

        // 2️⃣ Optimistic update
        FileNode updated = new FileNode(
                node.id,
                newName,
                node.mimeType,
                node.sizeBytes,
                System.currentTimeMillis()
        );

        cache.replaceNode(node.id, updated);
        notifyUpdated(parentId, updated);

        // 3️⃣ Simulate failure after 2 seconds
        main.postDelayed(() -> {

            // 4️⃣ Rollback
            cache.replaceNode(node.id, node);
            notifyUpdated(parentId, node);

            // 5️⃣ Notify error
            notifyError(new RuntimeException("Rename failed (simulated)"));

        }, 2000);
    }

    public void deleteNode(FileNode node) {
        if (node == null) return;

        String parentId = cache.getParent(node.id);
        if (parentId == null) return;

        // 1️⃣ Snapshot entire subtree
        FilesCache.Subtree snapshot = cache.exportSubtree(node.id);

        // 2️⃣ Optimistic delete
        cache.deleteNode(node.id);
        notifyRemoved(parentId, node.id);

        // 3️⃣ Simulate failure
        main.postDelayed(() -> {

            // 4️⃣ Restore full subtree
            cache.importSubtree(snapshot);
            notifyAdded(parentId, node);

            notifyError(new RuntimeException("Delete failed (simulated)"));

        }, 2000);
    }

    private String addFolder(String parent, String name) {
        FileNode node = new FileNode(UUID.randomUUID().toString(), name, FOLDER_MIME, 0, System.currentTimeMillis());
        cache.addNode(parent, node);
        return node.id;
    }

    private void addFile(String parent, String name, String mime, long size) {
        FileNode node = new FileNode(UUID.randomUUID().toString(), name, mime, size, System.currentTimeMillis());
        cache.addNode(parent, node);
    }

    /* ================= Notify ================= */

    private void notifyReady() {
        main.post(() -> {
            for (Listener l : listeners) l.onReady(rootId);
        });
    }

    private void notifyFolder(String id) {
        List<FileNode> children = cache.getChildren(id);
        main.post(() -> {
            for (Listener l : listeners) l.onFolderChanged(id, children);
        });
    }

    private void notifyAdded(String parentId, FileNode node) {
        main.post(() -> {
            for (Listener l : listeners) l.onFileAdded(parentId, node);
        });
    }

    private void notifyRemoved(String parentId, String nodeId) {
        main.post(() -> {
            for (Listener l : listeners) l.onFileRemoved(parentId, nodeId);
        });
    }

    private void notifyUpdated(String parentId, FileNode node) {
        main.post(() -> {
            for (Listener l : listeners) l.onFileUpdated(parentId, node);
        });
    }

    private void notifyError(Exception e) {
        main.post(() -> {
            for (Listener l : listeners) l.onError(e);
        });
    }

    /* ================= Listeners ================= */

    public void addListener(Listener l) {
        if (l != null) listeners.add(l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }
}