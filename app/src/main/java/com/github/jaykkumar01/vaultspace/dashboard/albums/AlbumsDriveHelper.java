package com.github.jaykkumar01.vaultspace.dashboard.albums;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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

    private final Drive drive;
    private final VaultSessionCache cache;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onSuccess(List<AlbumInfo> albums);
        void onEmpty();
        void onError(Exception e);
    }

    public AlbumsDriveHelper(Context context) {
        UserSession session = new UserSession(context);
        this.cache = session.getVaultCache();

        String primaryEmail = session.getPrimaryAccountEmail();
        this.drive = DriveClientProvider.forAccount(context, primaryEmail);

        Log.d(TAG, "Initialized for " + primaryEmail);
    }

    public void fetchAlbums(
            ExecutorService executor,
            Callback callback
    ) {
        executor.execute(() -> {
            try {
                if (cache.hasAlbumListCached()) {
                    List<AlbumInfo> cached = cache.getAlbums();
                    Log.d(TAG, "Using cached albums: " + cached.size());
                    postResult(callback, cached);
                    return;
                }

                String rootId = DriveFolderRepository.findRootFolderId(drive);
                if (rootId == null) {
                    cache.setAlbums(List.of());
                    postEmpty(callback);
                    return;
                }

                String query =
                        "'" + rootId + "' in parents " +
                                "and mimeType='application/vnd.google-apps.folder' " +
                                "and trashed=false";

                FileList result =
                        drive.files()
                                .list()
                                .setQ(query)
                                .setFields("files(id,name,createdTime,modifiedTime)")
                                .setOrderBy("modifiedTime desc")
                                .execute();

                List<File> files = result.getFiles();
                if (files == null || files.isEmpty()) {
                    cache.setAlbums(List.of());
                    postEmpty(callback);
                    return;
                }

                List<AlbumInfo> albums = new ArrayList<>(files.size());
                for (File f : files) {
                    albums.add(new AlbumInfo(
                            f.getId(),
                            f.getName(),
                            f.getCreatedTime().getValue(),
                            f.getModifiedTime().getValue()
                    ));
                }

                cache.setAlbums(albums);
                postResult(callback, albums);

            } catch (Exception e) {
                Log.e(TAG, "Failed to fetch albums", e);
                postError(callback, e);
            }
        });
    }

    public void invalidateCache() {
        cache.invalidateAlbums();
        Log.d(TAG, "Album cache invalidated");
    }

    /* ---------------- UI-thread dispatch ---------------- */

    private void postResult(Callback cb, List<AlbumInfo> albums) {
        mainHandler.post(() -> {
            if (albums.isEmpty()) cb.onEmpty();
            else cb.onSuccess(albums);
        });
    }

    private void postEmpty(Callback cb) {
        mainHandler.post(cb::onEmpty);
    }

    private void postError(Callback cb, Exception e) {
        mainHandler.post(() -> cb.onError(e));
    }
}
