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

    private static String rootFolderId;
    private static String albumsRootId;
    private static String filesRootId;
    private static String thumbnailsRootId;

    private DriveFolderRepository() {
    }

    /* ------------ Public API ------------ */

    public static String getRootFolderId(Context context) throws Exception {
        if (rootFolderId != null) return rootFolderId;
        Drive drive = DriveClientProvider.getPrimaryDrive(context);
        rootFolderId = resolveFolder(drive, ROOT_FOLDER_NAME, null);
        return rootFolderId;
    }

    public static String getAlbumsRootId(Context context) throws Exception {
        if (albumsRootId != null) return albumsRootId;
        Drive drive = DriveClientProvider.getPrimaryDrive(context);
        albumsRootId = resolveFolder(drive, ALBUMS_FOLDER_NAME, getRootFolderId(context));
        return albumsRootId;
    }

    public static String getFilesRootId(Context context) throws Exception {
        if (filesRootId != null) return filesRootId;
        Drive drive = DriveClientProvider.getPrimaryDrive(context);
        filesRootId = resolveFolder(drive, FILES_FOLDER_NAME, getRootFolderId(context));
        return filesRootId;
    }
    public static synchronized String getThumbnailsRootId(Context context) throws Exception {
        if (thumbnailsRootId != null) return thumbnailsRootId;
        Drive drive = DriveClientProvider.getPrimaryDrive(context);
        thumbnailsRootId = resolveFolder(drive, THUMBNAILS_FOLDER_NAME, getRootFolderId(context));
        return thumbnailsRootId;
    }



    /* ------------ Core Resolver ------------ */

    private static String resolveFolder(Drive drive, String name, String parentId) throws Exception {
        String id = findFolderId(drive, name, parentId);
        if (id != null) return id;
        return createFolder(drive, name, parentId).getId();
    }

    /* ------------ Drive Primitives ------------ */

    public static File createFolder(Drive drive, String name, String parentId) throws Exception {
        File folder = new File().setName(name).setMimeType(FOLDER_MIME);
        if (parentId != null) folder.setParents(Collections.singletonList(parentId));
        return drive.files().create(folder).setFields("id,name,createdTime,modifiedTime").execute();
    }

    private static String findFolderId(Drive drive, String name, String parentId) throws Exception {
        StringBuilder q = new StringBuilder()
                .append("mimeType='").append(FOLDER_MIME).append("'")
                .append(" and name='").append(name).append("'")
                .append(" and trashed=false");
        if (parentId != null) q.append(" and '").append(parentId).append("' in parents");

        FileList list = drive.files().list()
                .setQ(q.toString())
                .setFields("files(id)")
                .setPageSize(1)
                .execute();

        return list.getFiles().isEmpty() ? null : list.getFiles().get(0).getId();
    }

    public static void onSessionCleared() {
        rootFolderId = null;
        albumsRootId = null;
        filesRootId = null;
        thumbnailsRootId = null;
    }

}
