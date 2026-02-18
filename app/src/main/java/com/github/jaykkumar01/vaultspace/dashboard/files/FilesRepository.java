package com.github.jaykkumar01.vaultspace.dashboard.files;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.core.session.cache.FilesCache;
import com.github.jaykkumar01.vaultspace.dashboard.files.drive.FilesDriveHelper;
import com.github.jaykkumar01.vaultspace.models.FileNode;

import java.util.*;

public final class FilesRepository {

    private static volatile FilesRepository INSTANCE;

    public static FilesRepository getInstance(Context c) {
        if (INSTANCE == null) synchronized (FilesRepository.class) {
            if (INSTANCE == null) INSTANCE = new FilesRepository(c);
        }
        return INSTANCE;
    }

    /* ================= Listener ================= */

    public interface Listener {
        void onReady(String rootId);

        void onFolderChanged(String folderId, List<FileNode> children);

        void onError(Exception e);
    }

    /* ================= Core ================= */

    private static final String FOLDER_MIME = "application/vnd.google-apps.folder";
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Set<Listener> listeners = new HashSet<>();
    private final FilesCache cache;
    // private final FilesDriveHelper driveHelper;
    private String rootId;

    private FilesRepository(Context c) {
        cache = new UserSession(c).getVaultCache().files;
        // driveHelper=new FilesDriveHelper(c);
    }

    /* ================= Initialize ================= */

    public void initialize() {
        try {
            // ===== REAL DRIVE (Commented) =====
            // rootId=driveHelper.resolveFilesRoot();
            // if(!cache.isInitialized()) cache.initialize(rootId);
            // driveHelper.fetchFolderChildren(...);

            // ===== FAKE MODE =====
            rootId = "root_fake";
            if (!cache.isInitialized()) cache.initialize(rootId);
            if (cache.getChildren(rootId).isEmpty()) buildDeepFakeTree();
            notifyReady();
            notifyFolder(rootId, cache.getChildren(rootId));

        } catch (Exception e) {
            notifyError(e);
        }
    }

    public String getRootId() {
        return rootId;
    }

    /* ================= Navigation ================= */

    public void openFolder(String folderId) {
        notifyFolder(folderId, cache.getChildren(folderId));
    }

    /* ================= Deep Fake Tree ================= */

    private void buildDeepFakeTree() {

        // Root level
        String documents = addFolder(rootId, "Documents");
        String media = addFolder(rootId, "Media");
        String work = addFolder(rootId, "Work");
        addFile(rootId, "readme.txt", "text/plain", 12_000);

        /* ---------- Documents ---------- */
        String personal = addFolder(documents, "Personal");
        String finance = addFolder(documents, "Finance");
        addFile(documents, "resume.pdf", "application/pdf", 240_000);

        addFile(personal, "diary.txt", "text/plain", 18_000);
        addFile(personal, "passport.pdf", "application/pdf", 500_000);

        String taxes = addFolder(finance, "Taxes");
        addFile(finance, "bank_statement.pdf", "application/pdf", 320_000);
        addFile(taxes, "tax_2023.pdf", "application/pdf", 410_000);
        addFile(taxes, "tax_2024.pdf", "application/pdf", 430_000);

        /* ---------- Media ---------- */
        String photos = addFolder(media, "Photos");
        String videos = addFolder(media, "Videos");
        addFile(media, "cover.png", "image/png", 2_000_000);

        String travel = addFolder(photos, "Travel");
        String family = addFolder(photos, "Family");

        addFile(travel, "goa.jpg", "image/jpeg", 3_200_000);
        addFile(travel, "manali.jpg", "image/jpeg", 2_900_000);
        addFile(family, "birthday.jpg", "image/jpeg", 3_100_000);

        String raw = addFolder(videos, "Raw");
        String edited = addFolder(videos, "Edited");

        addFile(raw, "clip1.mp4", "video/mp4", 45_000_000);
        addFile(raw, "clip2.mp4", "video/mp4", 38_000_000);
        addFile(edited, "vlog_final.mp4", "video/mp4", 62_000_000);

        /* ---------- Work ---------- */
        String projects = addFolder(work, "Projects");
        String reports = addFolder(work, "Reports");

        String vaultspace = addFolder(projects, "VaultSpace");
        String backend = addFolder(vaultspace, "Backend");
        String mobile = addFolder(vaultspace, "Mobile");

        addFile(backend, "DriveHelper.java", "text/x-java-source", 15_000);
        addFile(backend, "FilesRepository.java", "text/x-java-source", 18_000);
        addFile(mobile, "PlayerScreen.kt", "text/x-kotlin", 20_000);

        addFile(reports, "Q1_Report.pdf", "application/pdf", 800_000);
        addFile(reports, "Q2_Report.pdf", "application/pdf", 760_000);
    }

    /* ================= Fake Helpers ================= */

    private String addFolder(String parent, String name) {
        FileNode node = new FileNode(UUID.randomUUID().toString(), name, FOLDER_MIME, 0, System.currentTimeMillis());
        cache.addNode(parent, node);
        return node.id;
    }

    private void addFile(String parent, String name, String mime, long size) {
        FileNode node = new FileNode(UUID.randomUUID().toString(), name, mime, size, System.currentTimeMillis());
        cache.addNode(parent, node);
    }

    /* ================= Notifications ================= */

    private void notifyReady() {
        for (Listener l : listeners) l.onReady(rootId);
    }

    private void notifyFolder(String id, List<FileNode> children) {
        main.post(() -> {
            for (Listener l : listeners)
                l.onFolderChanged(id, children);
        });
    }

    private void notifyError(Exception e) {
        main.post(() -> {
            for (Listener l : listeners)
                l.onError(e);
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
