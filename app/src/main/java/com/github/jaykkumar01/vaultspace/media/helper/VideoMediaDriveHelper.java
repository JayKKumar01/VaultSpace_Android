package com.github.jaykkumar01.vaultspace.media.helper;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.core.auth.GoogleCredentialFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class VideoMediaDriveHelper {

    public interface Callback {
        void onReady(@NonNull DefaultMediaSourceFactory factory,
                     @NonNull String url);

        void onError(@NonNull Exception e);
    }

    private static final String TAG = "VideoMediaDriveHelper";

    private final GoogleAccountCredential credential;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public VideoMediaDriveHelper(@NonNull Context context) {
        this.credential = GoogleCredentialFactory.forPrimaryDrive(context);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /* ---------------- public API ---------------- */

    @OptIn(markerClass = UnstableApi.class)
    public void prepare(
            @NonNull AlbumMedia media,
            @NonNull Callback callback
    ) {
        executor.execute(() -> {
            try {
                // 🔑 Single blocking token fetch (safe on bg thread)
                String token = credential.getToken();
                if (token == null || token.isEmpty())
                    throw new IllegalStateException("Drive token missing");

                Map<String, String> headers = new HashMap<>(1);
                headers.put("Authorization", "Bearer " + token);

                DefaultHttpDataSource.Factory http =
                        new DefaultHttpDataSource.Factory()
                                .setAllowCrossProtocolRedirects(true)
                                .setDefaultRequestProperties(headers);

                DefaultMediaSourceFactory factory =
                        new DefaultMediaSourceFactory(
                                DriveSingleFileCacheHelper.wrap(
                                        credential.getContext(),
                                        media.fileId,
                                        http
                                )
                        );

                String url =
                        "https://www.googleapis.com/drive/v3/files/"
                                + media.fileId + "?alt=media";

                mainHandler.post(() -> callback.onReady(factory, url));

            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void release() {
        executor.shutdownNow();
    }
}
