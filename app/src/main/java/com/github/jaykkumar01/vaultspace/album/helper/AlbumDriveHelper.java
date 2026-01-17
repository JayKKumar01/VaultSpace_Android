package com.github.jaykkumar01.vaultspace.album.helper;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.album.AlbumMedia;
import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * AlbumDriveHelper
 *
 * Responsibilities:
 * - Fetch album media from Drive
 * - Deliver result asynchronously on main thread
 *
 * Non-responsibilities:
 * - Caching
 * - UI decisions
 * - State management
 */
public final class AlbumDriveHelper {

    private static final String TAG = "VaultSpace:AlbumDrive";

    private final String albumId;
    private final Drive primaryDrive;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /* ==========================================================
     * Callback
     * ========================================================== */

    public interface FetchCallback {
        void onResult(List<AlbumMedia> items);
        void onError(Exception e);
    }

    /* ==========================================================
     * Constructor
     * ========================================================== */

    public AlbumDriveHelper(@NonNull Context context, @NonNull String albumId) {
        this.albumId = albumId;

        UserSession session = new UserSession(context);
        String email = session.getPrimaryAccountEmail();
        primaryDrive = DriveClientProvider.forAccount(context, email);

        Log.d(TAG, "Initialized groupId=" + albumId + ", account=" + email);
    }

    /* ==========================================================
     * Fetch
     * ========================================================== */

    public void fetchAlbumMedia(
            ExecutorService executor,
            FetchCallback callback
    ) {
        Log.d(TAG, "fetchAlbumMedia start groupId=" + albumId);

        executor.execute(() -> {
            try {
                String q =
                        "'" + albumId + "' in parents " +
                                "and trashed=false";

                FileList list = primaryDrive.files().list()
                        .setQ(q)
                        .setFields(
                                "files(" +
                                        "id," +
                                        "name," +
                                        "mimeType," +
                                        "modifiedTime," +
                                        "size," +
                                        "thumbnailLink" +
                                        ")"
                        )
                        .setOrderBy("modifiedTime desc")
                        .execute();

                List<AlbumMedia> media = mapToAlbumMedia(list);

                Log.d(
                        TAG,
                        "fetch success groupId=" + albumId +
                                ", itemCount=" + media.size()
                );

                postResult(callback, media);

            } catch (Exception e) {
                Log.e(TAG, "fetchAlbumMedia failed groupId=" + albumId, e);
                postError(callback, e);
            }
        });
    }

    /* ==========================================================
     * Mapping
     * ========================================================== */

    private static List<AlbumMedia> mapToAlbumMedia(FileList list) {
        List<AlbumMedia> media = new ArrayList<>();

        if (list.getFiles() != null) {
            for (File f : list.getFiles()) {
                media.add(new AlbumMedia(
                        f.getId(),
                        f.getName(),
                        f.getMimeType(),
                        f.getModifiedTime() != null
                                ? f.getModifiedTime().getValue()
                                : 0L,
                        f.getSize() != null
                                ? f.getSize()
                                : 0L,
                        f.getThumbnailLink()
                ));
            }
        }

        return media;
    }

    /* ==========================================================
     * Main-thread delivery
     * ========================================================== */

    private void postResult(FetchCallback cb, List<AlbumMedia> items) {
        Log.d(TAG, "postResult → main thread groupId=" + albumId);
        mainHandler.post(() -> cb.onResult(items));
    }

    private void postError(FetchCallback cb, Exception e) {
        Log.d(TAG, "postError → main thread groupId=" + albumId);
        mainHandler.post(() -> cb.onError(e));
    }
}
