package com.github.jaykkumar01.vaultspace.album;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.core.session.VaultSessionCache;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public final class AlbumDriveHelper {

    private static final String TAG = "VaultSpace:AlbumDrive";

    private final Drive primaryDrive;
    private final VaultSessionCache cache;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final String albumId;

    public interface FetchCallback {
        void onResult(List<AlbumMedia> items);
        void onError(Exception e);
    }

    public AlbumDriveHelper(Context context, String albumId) {
        this.albumId = albumId;

        UserSession session = new UserSession(context);
        cache = session.getVaultCache();

        String email = session.getPrimaryAccountEmail();
        primaryDrive = DriveClientProvider.forAccount(context, email);

        Log.d(TAG, "Initialized for album: " + albumId);
    }

    /* ---------------- Fetch ---------------- */

    public void fetchAlbumItems(ExecutorService executor, FetchCallback callback) {
        executor.execute(() -> {
            try {
                if (cache.albumMedia.hasAlbumMediaCached(albumId)) {
                    List<AlbumMedia> cached = cache.albumMedia.getAlbumMedia(albumId);
                    Log.d(TAG, "Cache HIT for album: " + albumId + " (" + cached.size() + ")");
                    postResult(callback, cached);
                    return;
                }


                Log.d(TAG, "Cache MISS for album: " + albumId);

                String q = "'" + albumId + "' in parents and trashed=false";

                FileList list = primaryDrive.files()
                        .list()
                        .setQ(q)
                        .setFields("files(id,name,mimeType,modifiedTime,size,thumbnailLink)")
                        .setOrderBy("modifiedTime desc")
                        .execute();

                List<AlbumMedia> items = mapItems(list);
                cache.albumMedia.setAlbumMedia(albumId, items);

                Log.d(TAG, "Fetched & cached " + items.size() + " items for album: " + albumId);
                postResult(callback, items);

            } catch (Exception e) {
                Log.e(TAG, "Failed to fetch album items: " + albumId, e);
                postError(callback, e);
            }
        });
    }


    /* ---------------- Mapping ---------------- */

    private static List<AlbumMedia> mapItems(FileList list) {
        List<AlbumMedia> items = new ArrayList<>();
        if (list.getFiles() == null) return items;

        for (File f : list.getFiles()) {
            AlbumMedia item = new AlbumMedia(
                    f.getId(),
                    f.getName(),
                    f.getMimeType(),
                    f.getModifiedTime() != null ? f.getModifiedTime().getValue() : 0L,
                    f.getSize() != null ? f.getSize() : 0L,
                    f.getThumbnailLink()
            );
            items.add(item);
        }
        return items;
    }

    /* ---------------- Utils ---------------- */

    private void postResult(FetchCallback cb, List<AlbumMedia> items) {
        mainHandler.post(() -> cb.onResult(items));
    }

    private void postError(FetchCallback cb, Exception e) {
        mainHandler.post(() -> cb.onError(e));
    }

    /* ---------------- Cache ---------------- */

    public void invalidateCache() {
        cache.albumMedia.invalidateAlbumMedia(albumId);
        Log.d(TAG, "Album items cache invalidated: " + albumId);
    }

}
