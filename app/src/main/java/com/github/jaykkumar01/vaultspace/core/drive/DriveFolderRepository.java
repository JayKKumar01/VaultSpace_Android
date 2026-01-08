package com.github.jaykkumar01.vaultspace.core.drive;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.Collections;

public final class DriveFolderRepository {

    private static final String FOLDER_MIME =
            "application/vnd.google-apps.folder";
    private static final String ROOT_FOLDER_NAME = "VaultSpace";

    private DriveFolderRepository() {}

    /* ---------------- Root Folder ---------------- */

    /** Read-only: returns null if root does not exist */
    public static String findRootFolderId(Drive drive) throws Exception {
        return findFolderId(drive, ROOT_FOLDER_NAME, null);
    }

    /** Write path: creates root if missing */
    public static String getOrCreateRootFolder(Drive drive) throws Exception {
        return getOrCreateFolder(drive, ROOT_FOLDER_NAME, null);
    }

    /* ---------------- Generic ---------------- */

    public static String getOrCreateFolder(
            Drive drive,
            String folderName,
            String parentId
    ) throws Exception {

        String folderId = findFolderId(drive, folderName, parentId);
        if (folderId != null) return folderId;

        File folder = new File()
                .setName(folderName)
                .setMimeType(FOLDER_MIME);

        if (parentId != null) {
            folder.setParents(Collections.singletonList(parentId));
        }

        return drive.files()
                .create(folder)
                .setFields("id")
                .execute()
                .getId();
    }

    public static String findFolderId(
            Drive drive,
            String folderName,
            String parentId
    ) throws Exception {

        StringBuilder q = new StringBuilder();
        q.append("mimeType='").append(FOLDER_MIME).append("'");
        q.append(" and name='").append(folderName).append("'");
        q.append(" and trashed=false");

        if (parentId != null) {
            q.append(" and '").append(parentId).append("' in parents");
        }

        FileList list =
                drive.files()
                        .list()
                        .setQ(q.toString())
                        .setFields("files(id)")
                        .setPageSize(1)
                        .execute();

        return list.getFiles().isEmpty()
                ? null
                : list.getFiles().get(0).getId();
    }
}
