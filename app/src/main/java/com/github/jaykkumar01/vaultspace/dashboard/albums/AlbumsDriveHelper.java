package com.github.jaykkumar01.vaultspace.dashboard.albums;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.github.jaykkumar01.vaultspace.core.drive.DriveFolderRepository;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.core.session.VaultSessionCache;
import com.github.jaykkumar01.vaultspace.models.AlbumInfo;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public final class AlbumsDriveHelper {

    private static final String TAG = "VaultSpace:AlbumsDrive";

    private final Drive primaryDrive;
    private final VaultSessionCache cache;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface FetchCallback {
        void onResult(List<AlbumInfo> albums);
        void onError(Exception e);
    }

    public interface CreateAlbumCallback {
        void onSuccess(AlbumInfo album);
        void onError(Exception e);
    }

    public AlbumsDriveHelper(Context context) {
        UserSession session = new UserSession(context);
        cache = session.getVaultCache();
        String email = session.getPrimaryAccountEmail();
        primaryDrive = DriveClientProvider.forAccount(context, email);
        Log.d(TAG, "Initialized primaryDrive for " + email);
    }

    public void fetchAlbums(ExecutorService executor, FetchCallback callback) {
        executor.execute(() -> {
            try {
                if (cache.hasAlbumListCached()) {
                    postResult(callback, cache.getAlbums());
                    return;
                }

                String albumsRootId =
                        DriveFolderRepository.findAlbumsRootId(primaryDrive);

                if (albumsRootId == null) {
                    cache.setAlbums(List.of());
                    postResult(callback, List.of());
                    return;
                }

                String q =
                        "'" + albumsRootId + "' in parents " +
                                "and mimeType='application/vnd.google-apps.folder' " +
                                "and trashed=false";

                FileList list = primaryDrive.files().list()
                        .setQ(q)
                        .setFields("files(id,name,createdTime,modifiedTime)")
                        .setOrderBy("modifiedTime desc")
                        .execute();

                List<AlbumInfo> albums = getAlbums(list);

                cache.setAlbums(albums);
                postResult(callback, albums);

            } catch (Exception e) {
                Log.e(TAG, "Failed to fetch albums", e);
                postError(callback, e);
            }
        });
    }

    @NonNull
    private static List<AlbumInfo> getAlbums(FileList list) {
        List<AlbumInfo> albums = new ArrayList<>();
        List<File> files = list.getFiles();

        if (files != null) {
            for (File f : files) {
                albums.add(new AlbumInfo(
                        f.getId(),
                        f.getName(),
                        f.getCreatedTime().getValue(),
                        f.getModifiedTime().getValue(),
                        null
                ));
            }
        }
        return albums;
    }

    public void createAlbum(
            ExecutorService executor,
            String albumName,
            CreateAlbumCallback callback
    ) {
        executor.execute(() -> {
            try {
                String albumsRootId =
                        DriveFolderRepository.getOrCreateAlbumsRootId(primaryDrive);

                File created =
                        DriveFolderRepository.createFolder(
                                primaryDrive, albumName, albumsRootId
                        );

                AlbumInfo album = new AlbumInfo(
                        created.getId(),
                        created.getName(),
                        created.getCreatedTime().getValue(),
                        created.getModifiedTime().getValue(),
                        null
                );

                cache.addAlbum(album);
                mainHandler.post(() -> callback.onSuccess(album));

            } catch (Exception e) {
                Log.e(TAG, "Failed to create album", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void invalidateCache() {
        cache.invalidateAlbums();
    }

    private void postResult(FetchCallback cb, List<AlbumInfo> albums) {
        mainHandler.post(() -> cb.onResult(albums));
    }

    private void postError(FetchCallback cb, Exception e) {
        mainHandler.post(() -> cb.onError(e));
    }
}
