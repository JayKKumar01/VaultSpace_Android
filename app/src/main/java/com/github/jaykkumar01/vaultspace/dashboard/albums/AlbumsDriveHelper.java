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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AlbumsDriveHelper {

    private static final String TAG = "VaultSpace:AlbumsDrive";
    private static final long OP_TIMEOUT_MS = 10_000;

    private final Drive primaryDrive;
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

    public AlbumsDriveHelper(Context context) {
        UserSession session = new UserSession(context);
        String email = session.getPrimaryAccountEmail();
        primaryDrive = DriveClientProvider.forAccount(context, email);
        Log.d(TAG, "Initialized primaryDrive for " + email);
    }

    /* ==========================================================
     * Fetch albums (ALWAYS Drive)
     * ========================================================== */

    public void fetchAlbums(
            ExecutorService executor,
            FetchCallback callback
    ) {
        executor.execute(() -> {
            try {
                String rootId =
                        DriveFolderRepository.findAlbumsRootId(primaryDrive);

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
            ExecutorService executor,
            String albumName,
            CreateAlbumCallback callback
    ) {
        String trimmed = albumName == null ? "" : albumName.trim();
        if (trimmed.isEmpty()) {
            callback.onError(
                    new IllegalArgumentException("Album name is empty")
            );
            return;
        }

        AtomicBoolean completed = new AtomicBoolean(false);
        Runnable timeout = () -> failOnce(
                completed, callback,
                new TimeoutException("Album creation timed out")
        );
        mainHandler.postDelayed(timeout, OP_TIMEOUT_MS);

        executor.execute(() -> {
            try {
                String rootId =
                        DriveFolderRepository.getOrCreateAlbumsRootId(primaryDrive);

                File created = DriveFolderRepository.createFolder(
                        primaryDrive, trimmed, rootId
                );

                AlbumInfo album = toAlbumInfo(created);

                succeedOnce(completed, timeout, () ->
                        callback.onSuccess(album)
                );

            } catch (Exception e) {
                Log.e(TAG, "Failed to create album", e);
                failOnce(completed, callback, e);
            }
        });
    }

    /* ==========================================================
     * Rename album
     * ========================================================== */

    public void renameAlbum(
            ExecutorService executor,
            String albumId,
            String newName,
            RenameAlbumCallback callback
    ) {
        AtomicBoolean completed = new AtomicBoolean(false);
        Runnable timeout = () -> failOnce(
                completed, callback,
                new TimeoutException("Album rename timed out")
        );
        mainHandler.postDelayed(timeout, OP_TIMEOUT_MS);

        executor.execute(() -> {
            try {
                File update = new File();
                update.setName(newName);

                File updated = primaryDrive.files()
                        .update(albumId, update)
                        .setFields("id,name,createdTime,modifiedTime")
                        .execute();

                AlbumInfo album = toAlbumInfo(updated);

                succeedOnce(completed, timeout, () ->
                        callback.onSuccess(album)
                );

            } catch (Exception e) {
                Log.e(TAG, "Failed to rename album " + albumId, e);
                failOnce(completed, callback, e);
            }
        });
    }

    /* ==========================================================
     * Delete album
     * ========================================================== */

    public void deleteAlbum(
            ExecutorService executor,
            String albumId,
            DeleteAlbumCallback callback
    ) {
        AtomicBoolean completed = new AtomicBoolean(false);
        Runnable timeout = () -> failOnce(
                completed, callback,
                new TimeoutException("Album deletion timed out")
        );
        mainHandler.postDelayed(timeout, OP_TIMEOUT_MS);

        executor.execute(() -> {
            try {
                primaryDrive.files().delete(albumId).execute();
                succeedOnce(completed, timeout, () ->
                        callback.onSuccess(albumId)
                );
            } catch (Exception e) {
                Log.e(TAG, "Failed to delete album " + albumId, e);
                failOnce(completed, callback, e);
            }
        });
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

    private void postError(FetchCallback cb, Exception e) {
        mainHandler.post(() -> cb.onError(e));
    }

    private <T> void succeedOnce(
            AtomicBoolean completed,
            Runnable timeout,
            Runnable success
    ) {
        if (completed.compareAndSet(false, true)) {
            mainHandler.removeCallbacks(timeout);
            mainHandler.post(success);
        }
    }

    private <T> void failOnce(
            AtomicBoolean completed,
            Object callback,
            Exception e
    ) {
        if (completed.compareAndSet(false, true)) {
            mainHandler.post(() -> {
                if (callback instanceof FetchCallback) {
                    ((FetchCallback) callback).onError(e);
                } else if (callback instanceof CreateAlbumCallback) {
                    ((CreateAlbumCallback) callback).onError(e);
                } else if (callback instanceof RenameAlbumCallback) {
                    ((RenameAlbumCallback) callback).onError(e);
                } else if (callback instanceof DeleteAlbumCallback) {
                    ((DeleteAlbumCallback) callback).onError(e);
                }
            });
        }
    }
}
