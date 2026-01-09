package com.github.jaykkumar01.vaultspace.core.drive;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.Collections;

public final class DriveFolderRepository {

    private static final String FOLDER_MIME = "application/vnd.google-apps.folder";
    private static final String ROOT_FOLDER_NAME = "VaultSpace";
    private static final String ALBUMS_FOLDER_NAME = "Albums";
    private static final String FILES_FOLDER_NAME = "Files";

    private DriveFolderRepository() {}

    /* ---------------- Root ---------------- */

    public static String findRootFolderId(Drive drive) throws Exception {
        return findFolderId(drive, ROOT_FOLDER_NAME, null);
    }

    public static String getOrCreateRootFolder(Drive drive) throws Exception {
        return getOrCreateFolder(drive, ROOT_FOLDER_NAME, null);
    }

    /* ---------------- Albums ---------------- */

    public static String findAlbumsRootId(Drive drive) throws Exception {
        String rootId = findRootFolderId(drive);
        return rootId == null ? null : findFolderId(drive, ALBUMS_FOLDER_NAME, rootId);
    }

    public static String getOrCreateAlbumsRootId(Drive drive) throws Exception {
        String rootId = getOrCreateRootFolder(drive);
        return getOrCreateFolder(drive, ALBUMS_FOLDER_NAME, rootId);
    }


    /* ---------------- Files ---------------- */

    public static String findFilesRootId(Drive drive) throws Exception {
        String rootId = findRootFolderId(drive);
        return rootId == null ? null : findFolderId(drive, FILES_FOLDER_NAME, rootId);
    }

    public static String getOrCreateFilesRootId(Drive drive) throws Exception {
        String rootId = getOrCreateRootFolder(drive);
        return getOrCreateFolder(drive, FILES_FOLDER_NAME, rootId);
    }

    /* ---------------- Reusable create ---------------- */

    public static File createFolder(Drive drive, String name, String parentId) throws Exception {
        File folder = new File().setName(name).setMimeType(FOLDER_MIME);
        if (parentId != null) folder.setParents(Collections.singletonList(parentId));
        return drive.files().create(folder).setFields("id,name,createdTime,modifiedTime").execute();
    }

    /* ---------------- Generic ---------------- */

    public static String getOrCreateFolder(Drive drive, String name, String parentId) throws Exception {
        String id = findFolderId(drive, name, parentId);
        if (id != null) return id;
        return createFolder(drive, name, parentId).getId();
    }

    public static String findFolderId(Drive drive, String name, String parentId) throws Exception {
        StringBuilder q = new StringBuilder();
        q.append("mimeType='").append(FOLDER_MIME).append("'")
                .append(" and name='").append(name).append("'")
                .append(" and trashed=false");

        if (parentId != null) q.append(" and '").append(parentId).append("' in parents");

        FileList list = drive.files()
                .list()
                .setQ(q.toString())
                .setFields("files(id)")
                .setPageSize(1)
                .execute();

        return list.getFiles().isEmpty() ? null : list.getFiles().get(0).getId();
    }
}
