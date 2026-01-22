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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class AlbumsDriveHelper {

    private static final String TAG = "VaultSpace:AlbumsDrive";

    private final Drive primaryDrive;
    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final TrustedAccountsCache trustedAccountsCache;

    /* ================= Constructor ================= */

    public AlbumsDriveHelper(@NonNull Context context) {
        appContext = context.getApplicationContext();
        UserSession session = new UserSession(appContext);
        primaryDrive = DriveClientProvider.forAccount(
                appContext,
                session.getPrimaryAccountEmail()
        );
        trustedAccountsCache = session.getVaultCache().trustedAccounts;
        Log.d(TAG, "Initialized primaryDrive");
    }

    /* ================= Fetch ================= */

    public void fetchAlbums(
            @NonNull ExecutorService executor,
            @NonNull FetchCallback callback
    ) {
        executor.execute(() -> {
            try {
                String rootId = DriveFolderRepository.getAlbumsRootId(primaryDrive);
                if (rootId == null) {
                    post(() -> callback.onResult(List.of()));
                    return;
                }

                FileList list = primaryDrive.files().list()
                        .setQ("'" + rootId + "' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false")
                        .setFields("files(id,name,createdTime,modifiedTime)")
                        .setOrderBy("modifiedTime desc")
                        .execute();

                post(() -> callback.onResult(parseAlbums(list)));

            } catch (Exception e) {
                Log.e(TAG, "fetchAlbums failed", e);
                post(() -> callback.onError(e));
            }
        });
    }

    /* ================= Create ================= */

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
                File created = DriveFolderRepository.createFolder(
                        primaryDrive,
                        name,
                        DriveFolderRepository.getAlbumsRootId(primaryDrive)
                );
                post(() -> callback.onSuccess(toAlbumInfo(created)));
            } catch (Exception e) {
                Log.e(TAG, "createAlbum failed", e);
                post(() -> callback.onError(e));
            }
        });
    }

    /* ================= Rename ================= */

    public void renameAlbum(
            @NonNull ExecutorService executor,
            @NonNull String albumId,
            @NonNull String newName,
            @NonNull RenameAlbumCallback callback
    ) {
        executor.execute(() -> {
            try {
                File updated = primaryDrive.files()
                        .update(albumId, new File().setName(newName))
                        .setFields("id,name,createdTime,modifiedTime")
                        .execute();

                post(() -> callback.onSuccess(toAlbumInfo(updated)));
            } catch (Exception e) {
                Log.e(TAG, "renameAlbum failed", e);
                post(() -> callback.onError(e));
            }
        });
    }

    /* ================= Delete ================= */
    public void deleteAlbum(
            @NonNull ExecutorService executor,
            @NonNull String albumId,
            @NonNull DeleteProgressCallback cb,
            @NonNull AtomicBoolean cancelled
    ) {
        // Immediate UI visibility (0 progress)
        post(() -> cb.onStart(0));

        executor.execute(() -> {
            try {
                deleteInternal(albumId, cb, cancelled);

                if (!cancelled.get()) {
                    post(cb::onCompleted);
                }

            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                post(() -> cb.onError(e));
            }
        });
    }

    private void deleteInternal(
            String albumId,
            DeleteProgressCallback cb,
            AtomicBoolean cancelled
    ) throws Exception {

        FileList list = primaryDrive.files().list()
                .setQ("'" + albumId + "' in parents and trashed=false")
                .setFields("files(id,name,size,owners(emailAddress))")
                .execute();

        List<File> files = list.getFiles();
        if (files == null || files.isEmpty()) {
            if (cancelled.get()) throw new Exception("Delete cancelled");
            primaryDrive.files().delete(albumId).execute();
            return;
        }

        final int total = files.size();
        post(() -> cb.onStart(total));

        Map<String, List<File>> byOwner = new HashMap<>();
        for (File f : files) {
            if (f.getOwners() == null || f.getOwners().isEmpty()) continue;
            byOwner.computeIfAbsent(
                    f.getOwners().get(0).getEmailAddress(),
                    k -> new ArrayList<>()
            ).add(f);
        }

        AtomicInteger deleted = new AtomicInteger();
        int threads = Math.max(
                1,
                Math.min(byOwner.size(),
                        Math.min(Runtime.getRuntime().availableProcessors(), 4))
        );

        try (ExecutorService ownerExec = Executors.newFixedThreadPool(threads)) {

            List<Callable<Void>> tasks = new ArrayList<>();

            for (Map.Entry<String, List<File>> entry : byOwner.entrySet()) {
                tasks.add(() -> {
                    Drive drive = DriveClientProvider.forAccount(
                            appContext,
                            entry.getKey()
                    );

                    for (File f : entry.getValue()) {
                        if (cancelled.get())
                            throw new Exception("Delete cancelled");

                        drive.files().delete(f.getId()).execute();

                        int done = deleted.incrementAndGet();
                        post(() -> cb.onFileDeleting(f.getName(), done, total));

                        Long size = f.getSize();
                        if (size != null && size > 0) {
                            trustedAccountsCache.recordDeleteUsage(
                                    entry.getKey(),
                                    size
                            );
                        }
                    }
                    return null;
                });
            }

            ownerExec.invokeAll(tasks);
        }

        if (cancelled.get())
            throw new Exception("Delete cancelled");

        primaryDrive.files().delete(albumId).execute();
    }



    /* ================= Helpers ================= */

    private void post(Runnable r) { mainHandler.post(r); }

    private static List<AlbumInfo> parseAlbums(FileList list) {
        List<AlbumInfo> out = new ArrayList<>();
        if (list.getFiles() != null)
            for (File f : list.getFiles())
                out.add(toAlbumInfo(f));
        return out;
    }

    private static AlbumInfo toAlbumInfo(File f) {
        return new AlbumInfo(
                f.getId(),
                f.getName(),
                f.getCreatedTime().getValue(),
                f.getModifiedTime().getValue(),
                null
        );
    }

    /* ================= Callbacks ================= */

    public interface FetchCallback {
        void onResult(List<AlbumInfo> albums);
        void onError(Exception e);
    }

    public interface CreateAlbumCallback {
        void onSuccess(AlbumInfo album);
        void onError(Exception e);
    }

    public interface RenameAlbumCallback {
        void onSuccess(AlbumInfo updated);
        void onError(Exception e);
    }

    public interface DeleteAlbumCallback {
        void onSuccess(String albumId);
        void onError(Exception e);
    }
}
