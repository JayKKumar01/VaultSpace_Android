package com.github.jaykkumar01.vaultspace.media.helper;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.core.auth.GoogleCredentialFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class VideoMediaDriveHelper {

    public interface Callback {
        void onReady(@NonNull String url, @NonNull String token);
        void onError(@NonNull Exception e);
    }

    private final Context appContext;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public VideoMediaDriveHelper(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void resolve(@NonNull AlbumMedia media, @NonNull Callback cb) {
        executor.execute(() -> {
            try {
                String token = GoogleCredentialFactory
                        .forPrimaryDrive(appContext)
                        .getToken();

                if (token == null || token.isEmpty())
                    throw new IllegalStateException("Drive token missing");

                String url =
                        "https://www.googleapis.com/drive/v3/files/"
                                + media.fileId + "?alt=media";

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
