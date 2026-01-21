package com.github.jaykkumar01.vaultspace.dashboard.albums;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.github.jaykkumar01.vaultspace.core.drive.DriveFolderRepository;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.models.AlbumInfo;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AlbumsDriveHelper {

    private static final String TAG = "VaultSpace:AlbumsDrive";

    private final Drive primaryDrive;
    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /* ==========================================================
     * Callbacks
     * ========================================================== */

    public interface FetchCallback {
        void onResult(List<AlbumInfo> albums);
        void onError(Exception e);
    }

    public interface CreateAlbumCallback {
        void onSuccess(AlbumInfo album);
        void onError(Exception e);
    }

    public interface DeleteAlbumCallback {
        void onSuccess(String albumId);
        void onError(Exception e);
    }

    public interface RenameAlbumCallback {
        void onSuccess(AlbumInfo updated);
        void onError(Exception e);
    }

    /* ==========================================================
     * Constructor
     * ========================================================== */

    public AlbumsDriveHelper(@NonNull Context context) {
        appContext = context.getApplicationContext();
        UserSession session = new UserSession(appContext);
        String email = session.getPrimaryAccountEmail();
        primaryDrive = DriveClientProvider.forAccount(appContext, email);
        Log.d(TAG, "Initialized primaryDrive for " + email);
    }

    /* ==========================================================
     * Fetch albums
     * ========================================================== */

    public void fetchAlbums(
            @NonNull ExecutorService executor,
            @NonNull FetchCallback callback
    ) {
        executor.execute(() -> {
            try {
                String rootId = DriveFolderRepository.getAlbumsRootId(primaryDrive);
                if (rootId == null) {
                    postResult(callback, List.of());
                    return;
                }

                String q = "'" + rootId + "' in parents "
                        + "and mimeType='application/vnd.google-apps.folder' "
                        + "and trashed=false";

                FileList list = primaryDrive.files().list()
                        .setQ(q)
                        .setFields("files(id,name,createdTime,modifiedTime)")
                        .setOrderBy("modifiedTime desc")
                        .execute();

                postResult(callback, parseAlbums(list));

            } catch (Exception e) {
                Log.e(TAG, "Failed to fetch albums", e);
                postError(callback, e);
            }
        });
    }

    /* ==========================================================
     * Create album
     * ========================================================== */

    public void createAlbum(
            @NonNull ExecutorService executor,
            @NonNull String albumName,
            @NonNull CreateAlbumCallback callback
    ) {
        String name = albumName.trim();
        if (name.isEmpty()) {
            callback.onError(new IllegalArgumentException("Album name is empty"));
            return;
        }

        executor.execute(() -> {
            try {
                String rootId = DriveFolderRepository.getAlbumsRootId(primaryDrive);
                File created = DriveFolderRepository.createFolder(primaryDrive, name, rootId);
                postSuccess(callback, toAlbumInfo(created));
            } catch (Exception e) {
                Log.e(TAG, "Failed to create album", e);
                postError(callback, e);
            }
        });
    }

    /* ==========================================================
     * Rename album
     * ========================================================== */

    public void renameAlbum(
            @NonNull ExecutorService executor,
            @NonNull String albumId,
            @NonNull String newName,
            @NonNull RenameAlbumCallback callback
    ) {
        executor.execute(() -> {
            try {
                File update = new File().setName(newName);
                File updated = primaryDrive.files()
                        .update(albumId, update)
                        .setFields("id,name,createdTime,modifiedTime")
                        .execute();

                postSuccess(callback, toAlbumInfo(updated));

            } catch (Exception e) {
                Log.e(TAG, "Failed to rename album " + albumId, e);
                postError(callback, e);
            }
        });
    }

    /* ==========================================================
     * Delete album (owner-aware)
     * ========================================================== */

    public void deleteAlbum(
            @NonNull ExecutorService executor,
            @NonNull String albumId,
            @NonNull DeleteAlbumCallback callback
    ) {
        executor.execute(() -> {
            try {
                deleteAlbumContentsAndFolder(albumId);
                postSuccess(callback, albumId);
            } catch (Exception e) {
                Log.e(TAG, "Failed to delete album " + albumId, e);
                postError(callback, e);
            }
        });
    }

    private void deleteAlbumContentsAndFolder(@NonNull String albumId) throws Exception {

        FileList list = primaryDrive.files().list()
                .setQ("'" + albumId + "' in parents and trashed=false")
                .setFields("files(id,owners(emailAddress))")
                .execute();

        if (list.getFiles() == null || list.getFiles().isEmpty()) {
            primaryDrive.files().delete(albumId).execute();
            Log.d(TAG, "Album deleted");
            return;
        }

        Map<String, List<String>> byOwner = new HashMap<>();

        for (File f : list.getFiles()) {
            if (f.getOwners() == null || f.getOwners().isEmpty())
                throw new IllegalStateException("Missing owner for file " + f.getId());

            String owner = f.getOwners().get(0).getEmailAddress();
            byOwner.computeIfAbsent(owner, k -> new ArrayList<>()).add(f.getId());
        }

        int cpu = Runtime.getRuntime().availableProcessors();
        int threads = Math.max(1, Math.min(byOwner.size(), cpu - 1));

        try (ExecutorService executor =
                     Executors.newFixedThreadPool(threads)) {

            List<Callable<Void>> tasks = new ArrayList<>();

            for (var entry : byOwner.entrySet()) {
                String ownerEmail = entry.getKey();
                List<String> fileIds = entry.getValue();

                tasks.add(() -> {
                    Drive ownerDrive =
                            DriveClientProvider.forAccount(appContext, ownerEmail);

                    for (String fileId : fileIds) {
                        ownerDrive.files().delete(fileId).execute();
                    }
                    return null;
                });
            }

            tasks.add(() -> {
                primaryDrive.files().delete(albumId).execute();
                return null;
            });

            executor.invokeAll(tasks);
        }

        Log.d(TAG, "Album deleted");
    }


    /* ==========================================================
     * Helpers
     * ========================================================== */

    private static List<AlbumInfo> parseAlbums(FileList list) {
        List<AlbumInfo> albums = new ArrayList<>();
        if (list.getFiles() != null) {
            for (File f : list.getFiles()) {
                albums.add(toAlbumInfo(f));
            }
        }
        return albums;
    }

    private static AlbumInfo toAlbumInfo(File file) {
        return new AlbumInfo(
                file.getId(),
                file.getName(),
                file.getCreatedTime().getValue(),
                file.getModifiedTime().getValue(),
                null
        );
    }

    private void postResult(FetchCallback cb, List<AlbumInfo> albums) {
        mainHandler.post(() -> cb.onResult(albums));
    }

    private void postError(Object cb, Exception e) {
        mainHandler.post(() -> {
            if (cb instanceof FetchCallback) ((FetchCallback) cb).onError(e);
            else if (cb instanceof CreateAlbumCallback) ((CreateAlbumCallback) cb).onError(e);
            else if (cb instanceof RenameAlbumCallback) ((RenameAlbumCallback) cb).onError(e);
            else if (cb instanceof DeleteAlbumCallback) ((DeleteAlbumCallback) cb).onError(e);
        });
    }

    private void postSuccess(CreateAlbumCallback cb, AlbumInfo album) {
        mainHandler.post(() -> cb.onSuccess(album));
    }

    private void postSuccess(RenameAlbumCallback cb, AlbumInfo album) {
        mainHandler.post(() -> cb.onSuccess(album));
    }

    private void postSuccess(DeleteAlbumCallback cb, String albumId) {
        mainHandler.post(() -> cb.onSuccess(albumId));
    }
}
