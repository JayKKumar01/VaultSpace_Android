package com.github.jaykkumar01.vaultspace.album.helper;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.album.model.MediaGeometry;
import com.github.jaykkumar01.vaultspace.album.model.Moments;
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
    private final DriveResolver driveResolver;

    AlbumMediaFetcher(@NonNull Context context, @NonNull String albumId) {
        this.appContext = context.getApplicationContext();
        this.albumId = albumId;
        this.driveResolver = new DriveResolver(appContext);
    }

    public List<AlbumMedia> getMedia(Drive drive) throws Exception {
        long startMs = System.currentTimeMillis();
        Map<String, AlbumMedia> unique = new ConcurrentHashMap<>();

        fetchFromAccountInternal(drive, unique);

        List<AlbumMedia> result = new ArrayList<>(unique.values());
        result.sort((a, b) -> Long.compare(b.momentMillis, a.momentMillis));

        Log.d(TAG, "fetch single account albumId=" + albumId
                + " items=" + result.size()
                + " took=" + (System.currentTimeMillis() - startMs) + "ms");

        return result;
    }

    private void fetchFromAccountInternal(@NonNull Drive drive,
                                          @NonNull Map<String, AlbumMedia> out) throws Exception {

        String pageToken = null;
        int attempts = 0;

        do {
            try {
                FileList list = drive.files().list()
                        .setQ("'" + albumId + "' in parents and trashed=false")
                        .setFields("nextPageToken,files(id,name,mimeType,createdTime,modifiedTime,size,thumbnailLink,appProperties)")
                        .setPageSize(200)
                        .setPageToken(pageToken)
                        .execute();

                if (list.getFiles() != null) {
                    for (File f : list.getFiles()) {
                        out.computeIfAbsent(f.getId(), id -> createAlbumMedia(f));
                    }
                }

                pageToken = list.getNextPageToken();
                attempts = 0; // reset on success

            } catch (Exception e) {
                if (++attempts >= MAX_ATTEMPTS) throw e;
                SystemClock.sleep(RETRY_DELAY_MS);
            }
        } while (pageToken != null);
    }


    private AlbumMedia createAlbumMedia(@NonNull File file) {

        String thumbRef = driveResolver.resolve(file);

        Moments m = driveResolver.resolveMoments(file);
        MediaGeometry g = driveResolver.resolveGeometry(file);

        long duration = driveResolver.resolveDuration(file);

        UploadedItem item = new UploadedItem(
                file.getId(),
                file.getName(),
                file.getMimeType(),
                file.getSize() != null ? file.getSize() : 0L,
                m.originMoment,
                m.momentMillis,
                m.vsOrigin,
                g.aspectRatio,
                g.rotation,
                duration,        // 🟢 FIX
                thumbRef
        );



        return new AlbumMedia(item);
    }

}
