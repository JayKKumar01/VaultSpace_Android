package com.github.jaykkumar01.vaultspace.media.helper;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.core.auth.GoogleCredentialFactory;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class VideoMediaDriveHelper {

    public interface Callback {
        void onReady(@NonNull MediaSource mediaSource);
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

    @OptIn(markerClass = UnstableApi.class)
    public void buildMediaSource(@NonNull AlbumMedia media,
                                 @NonNull Callback callback) {

        executor.execute(() -> {
            try {
                String token = GoogleCredentialFactory
                        .forPrimaryDrive(appContext)
                        .getToken();

                if (token == null || token.isEmpty())
                    throw new IllegalStateException("Drive token missing");

                DefaultHttpDataSource.Factory httpFactory =
                        new DefaultHttpDataSource.Factory()
                                .setAllowCrossProtocolRedirects(true)
                                .setDefaultRequestProperties(
                                        Map.of(
                                                "Authorization", "Bearer " + token,
                                                "Accept-Encoding", "identity"
                                        )
                                );

                Uri uri = Uri.parse(
                        "https://www.googleapis.com/drive/v3/files/"
                                + media.fileId + "?alt=media"
                );

                MediaItem item = new MediaItem.Builder()
                        .setUri(uri)
                        .setMimeType(media.mimeType)
                        .build();

                MediaSource source =
                        new ProgressiveMediaSource.Factory(httpFactory)
                                .createMediaSource(item);

                mainHandler.post(() -> callback.onReady(source));

            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void release() {
        executor.shutdownNow();
    }
}
