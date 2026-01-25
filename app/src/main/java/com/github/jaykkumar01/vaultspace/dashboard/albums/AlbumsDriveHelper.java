package com.github.jaykkumar01.vaultspace.dashboard.albums;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.github.jaykkumar01.vaultspace.core.drive.DriveFolderRepository;
import com.github.jaykkumar01.vaultspace.core.drive.TrustedAccountsRepository;
import com.github.jaykkumar01.vaultspace.models.AlbumInfo;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class AlbumsDriveHelper {

    private static final String TAG = "VaultSpace:AlbumsDrive";

    private final Drive primaryDrive;
    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final TrustedAccountsRepository trustedAccountsRepository;

    /* ================= Constructor ================= */

    public AlbumsDriveHelper(@NonNull Context context) {
        appContext = context.getApplicationContext();
        primaryDrive = DriveClientProvider.getPrimaryDrive(appContext);
        this.trustedAccountsRepository = TrustedAccountsRepository.getInstance(context);
        Log.d(TAG, "Initialized primaryDrive");
    }

    /* ================= Fetch ================= */

    public void fetchAlbums(
            @NonNull ExecutorService executor,
            @NonNull FetchCallback callback
    ) {
        executor.execute(() -> {
            try {
                String rootId = DriveFolderRepository.getAlbumsRootId(appContext);
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
                        DriveFolderRepository.getAlbumsRootId(appContext)
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
            @NonNull DeleteAlbumCallback cb
    ) {

        executor.execute(() -> {
            try {
                deleteInternal(albumId, cb);
            } catch (Exception e) {
                post(() -> cb.onError(e));
            }
        });
    }

    private void deleteInternal(String albumId, DeleteAlbumCallback cb) throws Exception {

        Map<String, Drive> driveCache = new ConcurrentHashMap<>();
        String pageToken = null;
        AtomicInteger deletedCount = new AtomicInteger();

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {

            do {
                FileList list = primaryDrive.files().list()
                        .setQ("'" + albumId + "' in parents and trashed=false")
                        .setPageSize(200)
                        .setPageToken(pageToken)
                        .setFields("nextPageToken,files(id,name,size,owners(emailAddress),appProperties)")
                        .execute();

                List<File> files = list.getFiles();
                if (files != null && !files.isEmpty()) {

                    List<Future<?>> tasks = new ArrayList<>();

                    for (File f : files) {
                        tasks.add(pool.submit(() -> {

                            if (f.getOwners() == null || f.getOwners().isEmpty()) return null;

                            String ownerEmail = f.getOwners().get(0).getEmailAddress();
                            Drive drive = driveCache.computeIfAbsent(
                                    ownerEmail,
                                    e -> DriveClientProvider.forAccount(appContext, e)
                            );

                            Map<String,String> props = f.getAppProperties();
                            if (props != null) {
                                String thumbId = props.get("thumb");
                                if (thumbId != null) {
                                    try { drive.files().delete(thumbId).execute(); }
                                    catch (Exception ignored) {}
                                }
                            }

                            drive.files().delete(f.getId()).execute();

                            int count = deletedCount.incrementAndGet();
                            Log.d(TAG, "deleted=" + count + " file=" + f.getId());

                            Long size = f.getSize();
                            if (size != null && size > 0)
                                trustedAccountsRepository.recordDeleteUsage(ownerEmail, size);

                            return null;
                        }));
                    }

                    for (Future<?> task : tasks) {
                        try {
                            task.get();
                        } catch (ExecutionException e) {
                            Throwable cause = e.getCause();
                            if (cause instanceof Exception) throw (Exception) cause;
                            throw new Exception(cause);
                        }
                    }
                }

                pageToken = list.getNextPageToken();

            } while (pageToken != null);

            primaryDrive.files().delete(albumId).execute();
            Log.d(TAG, "album delete complete, totalFiles=" + deletedCount.get());
            post(() -> cb.onSuccess(albumId));
        }
    }








    /* ================= Helpers ================= */

    private void post(Runnable r) {
        mainHandler.post(r);
    }

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
