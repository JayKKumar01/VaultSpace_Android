package com.github.jaykkumar01.vaultspace.media.helper;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.core.auth.GoogleCredentialFactory;
import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class VideoMediaDriveHelper {

    public interface Callback {
        void onReady(@NonNull String url, @NonNull String token);

        void onError(@NonNull Exception e);
    }
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final GoogleAccountCredential credential;

    public VideoMediaDriveHelper(@NonNull Context context) {
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.credential = GoogleCredentialFactory.forPrimaryDrive(context);
    }

    public void resolve(@NonNull AlbumMedia media, @NonNull Callback cb) {
        executor.execute(() -> {
            try {
                String token = credential.getToken();

                if (token == null || token.isEmpty())
                    throw new IllegalStateException("Drive token missing");

                String url = "https://www.googleapis.com/drive/v3/files/" + media.fileId + "?alt=media";

                mainHandler.post(() -> cb.onReady(url, token));

            } catch (Exception e) {
                mainHandler.post(() -> cb.onError(e));
            }
        });
    }

    public void release() {
        executor.shutdownNow();
    }
}
