package com.github.jaykkumar01.vaultspace.dashboard;

import android.content.Context;
import android.util.Log;

import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.github.jaykkumar01.vaultspace.core.drive.DriveFolderRepository;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.core.session.VaultSessionCache;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;

public class AlbumsDriveHelper {

    private static final String TAG = "VaultSpace:AlbumsDrive";
    private static final String ALBUMS_FOLDER_NAME = "Albums";

    private final Drive drive;
    private final VaultSessionCache cache;

    public AlbumsDriveHelper(Context context) {
        UserSession session = new UserSession(context);
        this.cache = session.getVaultCache();

        String primaryEmail = session.getPrimaryAccountEmail();
        this.drive = DriveClientProvider.forAccount(context, primaryEmail);

        Log.d(TAG, "AlbumsDriveHelper initialized for " + primaryEmail);
    }

    /**
     * Read-only check.
     * Cached for the lifetime of the session.
     */
    public boolean hasAlbums() throws Exception {

        if (cache.hasAlbumsCached()) {
            boolean cached = cache.getHasAlbums();
            Log.d(TAG, "Using cached hasAlbums = " + cached);
            return cached;
        }

        Log.d(TAG, "Albums cache miss, checking Drive");

        String rootFolderId =
                DriveFolderRepository.findRootFolderId(drive);

        if (rootFolderId == null) {
            cache.setHasAlbums(false);
            Log.d(TAG, "Root folder not found");
            return false;
        }

        String albumsFolderId =
                DriveFolderRepository.findFolderId(
                        drive,
                        ALBUMS_FOLDER_NAME,
                        rootFolderId
                );

        if (albumsFolderId == null) {
            cache.setHasAlbums(false);
            Log.d(TAG, "Albums folder not found");
            return false;
        }

        FileList children =
                drive.files()
                        .list()
                        .setQ("'" + albumsFolderId + "' in parents and trashed=false")
                        .setFields("files(id)")
                        .setPageSize(1)
                        .execute();

        boolean hasAlbums = !children.getFiles().isEmpty();
        cache.setHasAlbums(hasAlbums);

        Log.d(TAG, "Albums present (Drive) = " + hasAlbums);
        return hasAlbums;
    }

    /* ---------------- Cache control (future writes) ---------------- */

    public void markAlbumsPresent() {
        cache.setHasAlbums(true);
        Log.d(TAG, "Albums cache marked present");
    }

    public void invalidateCache() {
        cache.invalidateAlbums();
        Log.d(TAG, "Albums cache invalidated");
    }
}
