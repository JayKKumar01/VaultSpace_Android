package com.github.jaykkumar01.vaultspace.album;

import android.content.Context;
import android.nfc.Tag;
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
        void onResult(List<AlbumItem> items);
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


    /* ---------------- TEMP: Test Upload ---------------- */

    public void createAndUploadTestImage(ExecutorService executor, Runnable onDone) {
        executor.execute(() -> {
            try {
                // Dummy binary data
                byte[] data = "VaultSpace test image".getBytes();

                com.google.api.client.http.ByteArrayContent content =
                        new com.google.api.client.http.ByteArrayContent(
                                "image/png",
                                data
                        );

                // 🔴 Explicitly attach to THIS albumId
                File metadata = new File();
                metadata.setName("vs_test_" + System.currentTimeMillis() + ".png");
                metadata.setMimeType("image/png");
                metadata.setParents(java.util.Collections.singletonList(albumId));

                File uploaded = primaryDrive.files()
                        .create(metadata, content)
                        .setFields("id,name,parents")
                        .execute();

                Log.d(TAG,
                        "Test image uploaded: " + uploaded.getName()
                                + " id=" + uploaded.getId()
                                + " parents=" + uploaded.getParents()
                );

                // Important: force next fetch to hit Drive
                cache.invalidateAlbumItems(albumId);

                mainHandler.post(onDone);

            } catch (Exception e) {
                Log.e(TAG, "Test upload failed", e);
            }
        });
    }

    /* ---------------- Fetch ---------------- */

    public void fetchAlbumItems(ExecutorService executor, FetchCallback callback) {
        executor.execute(() -> {
            try {
                if (cache.hasAlbumItemsCached(albumId)) {
                    List<AlbumItem> cached = cache.getAlbumItems(albumId);
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

                List<AlbumItem> items = mapItems(list);
                cache.setAlbumItems(albumId, items);

                Log.d(TAG, "Fetched & cached " + items.size() + " items for album: " + albumId);
                postResult(callback, items);

            } catch (Exception e) {
                Log.e(TAG, "Failed to fetch album items: " + albumId, e);
                postError(callback, e);
            }
        });
    }


    /* ---------------- Mapping ---------------- */

    private static List<AlbumItem> mapItems(FileList list) {
        List<AlbumItem> items = new ArrayList<>();
        if (list.getFiles() == null) return items;

        for (File f : list.getFiles()) {
            AlbumItem item = new AlbumItem(
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

    private void postResult(FetchCallback cb, List<AlbumItem> items) {
        mainHandler.post(() -> cb.onResult(items));
    }

    private void postError(FetchCallback cb, Exception e) {
        mainHandler.post(() -> cb.onError(e));
    }

    /* ---------------- Cache ---------------- */

    public void invalidateCache() {
        cache.invalidateAlbumItems(albumId);
        Log.d(TAG, "Album items cache invalidated: " + albumId);
    }
}
