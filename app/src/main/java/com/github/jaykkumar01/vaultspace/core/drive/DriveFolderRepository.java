package com.github.jaykkumar01.vaultspace.core.drive;

import android.content.Context;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.Collections;

public final class DriveFolderRepository {

    private static final String FOLDER_MIME = "application/vnd.google-apps.folder";
    private static final String ROOT_FOLDER_NAME = "VaultSpace";
    private static final String ALBUMS_FOLDER_NAME = "Albums";
    private static final String FILES_FOLDER_NAME = "Files";
    private static final String THUMBNAILS_FOLDER_NAME = "Media Registry";

    private static String rootFolderId, albumsRootId, filesRootId, thumbnailsRootId;

    private DriveFolderRepository() {}

    /* ------------ Public API ------------ */

    public static synchronized String getRootFolderId(Context c) throws Exception {
        if (rootFolderId != null) return rootFolderId;
        Drive d = DriveClientProvider.getPrimaryDrive(c);
        rootFolderId = resolveFolder(d, ROOT_FOLDER_NAME, null).getId();
        return rootFolderId;
    }

    public static synchronized String getAlbumsRootId(Context c) throws Exception {
        if (albumsRootId != null) return albumsRootId;
        Drive d = DriveClientProvider.getPrimaryDrive(c);
        albumsRootId = resolveFolder(d, ALBUMS_FOLDER_NAME, getRootFolderId(c)).getId();
        return albumsRootId;
    }

    public static synchronized String getFilesRootId(Context c) throws Exception {
        if (filesRootId != null) return filesRootId;
        Drive d = DriveClientProvider.getPrimaryDrive(c);
        filesRootId = resolveFolder(d, FILES_FOLDER_NAME, getRootFolderId(c)).getId();
        return filesRootId;
    }

    public static synchronized String getThumbnailsRootId(Context c) throws Exception {
        if (thumbnailsRootId != null) return thumbnailsRootId;
        Drive d = DriveClientProvider.getPrimaryDrive(c);
        thumbnailsRootId = resolveFolder(d, THUMBNAILS_FOLDER_NAME, getRootFolderId(c)).getId();
        return thumbnailsRootId;
    }

    /* ------------ Core Resolver ------------ */

    public static File resolveFolder(Drive d, String name, String parentId) throws Exception {
        File f = findFolder(d, name, parentId);
        return (f != null && f.getId() != null) ? f : createFolder(d, name, parentId);
    }

    /* ------------ Drive Primitives ------------ */

    private static File createFolder(Drive d, String name, String parentId) throws Exception {
        File f = new File().setName(name).setMimeType(FOLDER_MIME);
        if (parentId != null) f.setParents(Collections.singletonList(parentId));
        return d.files().create(f).setFields("id,name,createdTime,modifiedTime").execute();
    }

    private static File findFolder(Drive d, String name, String parentId) throws Exception {
        StringBuilder q = new StringBuilder()
                .append("mimeType='").append(FOLDER_MIME).append("'")
                .append(" and name='").append(name).append("'")
                .append(" and trashed=false")
                .append(" and 'me' in owners");
        if (parentId != null) q.append(" and '").append(parentId).append("' in parents");

        FileList list = d.files().list()
                .setQ(q.toString())
                .setFields("files(id,name,createdTime,parents)")
                .setPageSize(1)
                .execute();

        return list.getFiles().isEmpty() ? null : list.getFiles().get(0);
    }

    /* ------------ Lifecycle ------------ */

    public static void onSessionCleared() {
        rootFolderId = albumsRootId = filesRootId = thumbnailsRootId = null;
    }
}
