package com.github.jaykkumar01.vaultspace.album.helper;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.album.AlbumMedia;
import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.github.jaykkumar01.vaultspace.core.drive.DriveFolderRepository;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.google.api.services.drive.Drive;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * AlbumDriveHelper
 *
 * Orchestrates album media fetch:
 * - resolves trusted accounts
 * - delegates Drive work to AlbumMediaFetcher
 * - delivers results on main thread
 */
public final class AlbumDriveHelper {

    private static final String TAG = "VaultSpace:AlbumDrive";
    private final AlbumMediaFetcher fetcher;
    private final Drive primaryDrive;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface FetchCallback {
        void onResult(@NonNull List<AlbumMedia> items);
        void onError(@NonNull Exception e);
    }

    public AlbumDriveHelper(
            @NonNull Context context,
            @NonNull String albumId
    ) {
        Context appContext = context.getApplicationContext();
        UserSession userSession = new UserSession(context);
        this.primaryDrive = DriveClientProvider.forAccount(context,userSession.getPrimaryAccountEmail());
        this.fetcher = new AlbumMediaFetcher(appContext, albumId);
    }

    public void fetchAlbumMedia(
            @NonNull ExecutorService executor,
            @NonNull FetchCallback callback
    ) {
        Log.d(TAG, "fetch start");

        executor.execute(() -> {
            try {
                List<AlbumMedia> result = fetcher.getMedia(primaryDrive);
                postResult(callback, result);
            } catch (Exception e) {
                postError(callback, e);
            }
        });
    }

    private void postResult(FetchCallback cb, List<AlbumMedia> items) {
        mainHandler.post(() -> cb.onResult(items));
    }

    private void postError(FetchCallback cb, Exception e) {
        mainHandler.post(() -> cb.onError(e));
    }
}
