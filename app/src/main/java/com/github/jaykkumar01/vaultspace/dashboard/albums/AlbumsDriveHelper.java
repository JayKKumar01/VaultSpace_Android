package com.github.jaykkumar01.vaultspace.dashboard.albums;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.github.jaykkumar01.vaultspace.core.drive.DriveFolderRepository;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.core.session.cache.TrustedAccountsCache;
import com.github.jaykkumar01.vaultspace.models.AlbumInfo;
import com.github.jaykkumar01.vaultspace.views.creative.delete.DeleteProgressCallback;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class AlbumsDriveHelper {

    private static final String TAG = "VaultSpace:AlbumsDrive";

    private final Drive primaryDrive;
    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final TrustedAccountsCache trustedAccountsCache;


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
        trustedAccountsCache = session.getVaultCache().trustedAccounts;

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
            @NonNull DeleteProgressCallback callback,
            @NonNull AtomicBoolean cancelled
    ) {
        executor.execute(() -> {
            try {
                deleteAlbumContentsAndFolder(albumId, callback, cancelled);
                if (!cancelled.get()) post(callback::onCompleted);
            } catch (Exception e) {
                if (!cancelled.get()) post(() -> callback.onError(e));
            }
        });
    }

    private void deleteAlbumContentsAndFolder(
            @NonNull String albumId,
            @NonNull DeleteProgressCallback cb,
            @NonNull AtomicBoolean cancelled
    ) throws Exception {

        FileList list = primaryDrive.files().list()
                .setQ("'" + albumId + "' in parents and trashed=false")
                .setFields("files(id,name,size,owners(emailAddress))")
                .execute();

        List<File> files = list.getFiles();
        if (files == null || files.isEmpty()) {
            if (!cancelled.get()) {
                primaryDrive.files().delete(albumId).execute();
            }
            post(() -> cb.onStart(0));
            return;
        }

        final int total = files.size();
        post(() -> cb.onStart(total));

        /* ---------- Group files by owner ---------- */

        Map<String, List<File>> byOwner = new HashMap<>();
        for (File f : files) {
            if (f.getOwners() == null || f.getOwners().isEmpty()) continue;
            String owner = f.getOwners().get(0).getEmailAddress();
            byOwner.computeIfAbsent(owner, k -> new ArrayList<>()).add(f);
        }

        AtomicInteger deletedCount = new AtomicInteger(0);

        int cpu = Runtime.getRuntime().availableProcessors();
        int threads = Math.max(1, Math.min(byOwner.size(), Math.min(cpu, 4)));

        /* ---------- Parallel delete per owner ---------- */

        try (ExecutorService ownerExecutor =
                     Executors.newFixedThreadPool(threads)) {

            List<Callable<Void>> tasks = new ArrayList<>();

            for (Map.Entry<String, List<File>> entry : byOwner.entrySet()) {
                final String ownerEmail = entry.getKey();
                final List<File> ownerFiles = entry.getValue();

                tasks.add(() -> {
                    Drive ownerDrive =
                            DriveClientProvider.forAccount(appContext, ownerEmail);

                    for (File f : ownerFiles) {
                        if (cancelled.get()) return null;

                        ownerDrive.files().delete(f.getId()).execute();

                        int done = deletedCount.incrementAndGet();
                        post(() -> cb.onFileDeleting(f.getName(), done, total));

                        // Storage refund (safe, local, deterministic)
                        Long size = f.getSize();
                        if (size != null && size > 0) {
                            trustedAccountsCache.recordDeleteUsage(ownerEmail, size);
                        }
                    }
                    return null;
                });
            }

            ownerExecutor.invokeAll(tasks);
        }

        /* ---------- Delete album folder LAST ---------- */

        if (!cancelled.get()) {
            primaryDrive.files().delete(albumId).execute();
        }
    }



    private void post(Runnable r) {
        mainHandler.post(r);
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
