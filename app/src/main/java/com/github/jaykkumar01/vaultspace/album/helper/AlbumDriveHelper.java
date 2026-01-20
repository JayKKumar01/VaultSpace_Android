package com.github.jaykkumar01.vaultspace.album.helper;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.album.AlbumMedia;
import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.core.session.cache.TrustedAccountsCache;
import com.github.jaykkumar01.vaultspace.models.TrustedAccount;
import com.github.jaykkumar01.vaultspace.models.base.UploadedItem;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * AlbumDriveHelper
 *
 * Fetches album media from all trusted accounts,
 * de-duplicates by fileId, sorts by modifiedTime desc,
 * and delivers results on main thread.
 */
public final class AlbumDriveHelper {

    private static final String TAG = "VaultSpace:AlbumDrive";

    private final Context appContext;
    private final String albumId;
    private final TrustedAccountsCache trustedCache;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /* ========================================================== */

    public interface FetchCallback {
        void onResult(@NonNull List<AlbumMedia> items);
        void onError(@NonNull Exception e);
    }

    /* ========================================================== */

    public AlbumDriveHelper(@NonNull Context context, @NonNull String albumId) {
        this.appContext = context.getApplicationContext();
        this.albumId = albumId;
        this.trustedCache = new UserSession(appContext).getVaultCache().trustedAccounts;
    }

    /* ========================================================== */

    public void fetchAlbumMedia(@NonNull ExecutorService executor,
                                @NonNull FetchCallback callback) {

        Log.d(TAG, "fetch start albumId=" + albumId);

        executor.execute(() -> {
            try {
                Map<String, AlbumMedia> unique = new HashMap<>();

                for (TrustedAccount account : trustedCache.getAccountsView()) {

                    Drive drive = DriveClientProvider.forAccount(appContext, account.email);

                    FileList list = drive.files().list()
                            .setQ("'" + albumId + "' in parents and trashed=false")
                            .setFields("files(id,name,mimeType,modifiedTime,size,thumbnailLink,hasThumbnail)")
                            .execute();

                    if (list.getFiles() == null) continue;

                    for (File f : list.getFiles()) {
                        if (unique.containsKey(f.getId())) continue;

                        UploadedItem item = new UploadedItem(
                                f.getId(),
                                f.getName(),
                                f.getMimeType(),
                                f.getSize() != null ? f.getSize() : 0L,
                                f.getModifiedTime() != null ? f.getModifiedTime().getValue() : 0L,
                                f.getThumbnailLink()
                        );

                        unique.put(f.getId(), new AlbumMedia(item));
                    }
                }

                List<AlbumMedia> result = new ArrayList<>(unique.values());
                result.sort((a, b) -> Long.compare(b.modifiedTime, a.modifiedTime));

                Log.d(TAG, "fetch done albumId=" + albumId + ", totalItems=" + result.size());

                postResult(callback, result);

            } catch (Exception e) {
                postError(callback, e);
            }
        });
    }

    /* ========================================================== */

    private void postResult(FetchCallback cb, List<AlbumMedia> items) {
        mainHandler.post(() -> cb.onResult(items));
    }

    private void postError(FetchCallback cb, Exception e) {
        mainHandler.post(() -> cb.onError(e));
    }
}
