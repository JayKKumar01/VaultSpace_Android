package com.github.jaykkumar01.vaultspace.album.helper;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.album.AlbumMedia;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadedItem;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class AlbumMediaFetcher {

    private static final String TAG = "VaultSpace:AlbumFetch";
    private static final int MAX_ATTEMPTS = 2;
    private static final long RETRY_DELAY_MS = 250;

    private final Context appContext;
    private final String albumId;
    private final DriveThumbnailResolver thumbnailResolver;

    AlbumMediaFetcher(@NonNull Context context, @NonNull String albumId) {
        this.appContext = context.getApplicationContext();
        this.albumId = albumId;
        this.thumbnailResolver = new DriveThumbnailResolver(appContext);
    }

    public List<AlbumMedia> getMedia(Drive drive) throws Exception {
        long startMs = System.currentTimeMillis();
        Map<String, AlbumMedia> unique = new ConcurrentHashMap<>();

        fetchFromAccountInternal(drive, unique);

        List<AlbumMedia> result = new ArrayList<>(unique.values());
        result.sort((a, b) -> Long.compare(b.modifiedTime, a.modifiedTime));

        Log.d(TAG, "fetch single account albumId=" + albumId
                + " items=" + result.size()
                + " took=" + (System.currentTimeMillis() - startMs) + "ms");

        return result;
    }

    private void fetchFromAccountInternal(@NonNull Drive drive,
                                          @NonNull Map<String, AlbumMedia> out) throws Exception {
        int attempts = 0;
        Exception lastError = null;

        while (attempts < MAX_ATTEMPTS) {
            try {
                FileList list = drive.files().list()
                        .setQ("'" + albumId + "' in parents and trashed=false")
                        .setFields("files(id,name,mimeType,modifiedTime,size,thumbnailLink,appProperties)")
                        .execute();

                if (list.getFiles() != null) {
                    for (File f : list.getFiles()) {
                        out.computeIfAbsent(f.getId(),
                                id -> createAlbumMedia(f));
                    }
                }
                return;
            } catch (Exception e) {
                lastError = e;
                attempts++;
                Log.w(TAG, "fetch failed, attempt=" + attempts, e);

                if (attempts < MAX_ATTEMPTS) {
                    SystemClock.sleep(RETRY_DELAY_MS);
                }
            }
        }

        throw lastError;
    }

    private AlbumMedia createAlbumMedia(@NonNull File file) {

        String thumbRef = thumbnailResolver.resolve(file);
        UploadedItem item = new UploadedItem(
                file.getId(),
                file.getName(),
                file.getMimeType(),
                file.getSize() != null ? file.getSize() : 0L,
                file.getModifiedTime() != null ? file.getModifiedTime().getValue() : 0L,
                thumbRef
        );
        return new AlbumMedia(item);
    }
}
