package com.github.jaykkumar01.vaultspace.core.download.engine;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.core.auth.GoogleCredentialFactory;
import com.github.jaykkumar01.vaultspace.core.download.base.DownloadDelegate;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LegacyDownloadManager implements DownloadDelegate {

    private static final String DRIVE_URL = "https://www.googleapis.com/drive/v3/files/%s?alt=media";

    private final Context appContext;
    private final DownloadManager dm;
    private final ExecutorService executor;
    private final Handler main;

    public LegacyDownloadManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.dm = (DownloadManager) appContext.getSystemService(Context.DOWNLOAD_SERVICE);
        this.executor = Executors.newSingleThreadExecutor();
        this.main = new Handler(Looper.getMainLooper());
    }

    @Override
    public void enqueue(AlbumMedia media) {
        executor.execute(() -> {
            try {
                GoogleAccountCredential c =
                        GoogleCredentialFactory.forPrimaryDrive(appContext);

                String token = c.getToken();
                String url = String.format(DRIVE_URL, media.fileId);

                DownloadManager.Request r =
                        new DownloadManager.Request(Uri.parse(url))
                                .addRequestHeader("Authorization", "Bearer " + token)
                                .setTitle(media.name)
                                .setDescription("Downloading from VaultSpace")
                                .setNotificationVisibility(
                                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                .setAllowedOverMetered(true)
                                .setAllowedOverRoaming(true)
                                .setDestinationInExternalPublicDir(
                                        Environment.DIRECTORY_DOWNLOADS,
                                        media.name
                                );

                main.post(() -> dm.enqueue(r));

            } catch (Exception ignored) {
                // DownloadManager will surface failure if request never enqueues
            }
        });
    }

    @Override
    public void cancelAll() {
        // non-goal for legacy path
    }
}
