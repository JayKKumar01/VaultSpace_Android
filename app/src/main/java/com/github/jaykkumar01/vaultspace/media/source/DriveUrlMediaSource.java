package com.github.jaykkumar01.vaultspace.media.source;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.core.auth.GoogleCredentialFactory;
import com.github.jaykkumar01.vaultspace.media.base.VideoMediaPrepareCallback;
import com.github.jaykkumar01.vaultspace.media.base.VideoMediaSource;
import com.github.jaykkumar01.vaultspace.media.helper.DriveSingleFileCacheHelper;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@UnstableApi
public final class DriveUrlMediaSource implements VideoMediaSource {

    /* ---------------- dependencies ---------------- */

    private final Context context;
    private final GoogleAccountCredential credential;
    private final ExecutorService executor;
    private final Handler mainHandler;

    /* ---------------- state ---------------- */

    private final AtomicBoolean preparing = new AtomicBoolean(false);
    private volatile boolean released = false;

    // 🔑 cached Drive access token (in-memory)
    private volatile String cachedToken;

    /* ---------------- constructor ---------------- */

    public DriveUrlMediaSource(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.credential = GoogleCredentialFactory.forPrimaryDrive(this.context);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /* ---------------- VideoMediaSource ---------------- */

    @Override
    public void prepare(@NonNull AlbumMedia media,
                        @NonNull VideoMediaPrepareCallback callback) {

        if (released) return;
        if (!preparing.compareAndSet(false, true)) return;

        executor.execute(() -> {
            try {
                if (released) return;

                // 🔑 Fetch token only once per source lifetime
                String token = cachedToken;
                if (token == null) {
                    token = credential.getToken();
                    if (token == null || token.isEmpty())
                        throw new IllegalStateException("Drive token missing");
                    cachedToken = token;
                }

                Map<String, String> headers = new HashMap<>(1);
                headers.put("Authorization", "Bearer " + token);

                DefaultHttpDataSource.Factory http =
                        new DefaultHttpDataSource.Factory()
                                .setAllowCrossProtocolRedirects(true)
                                .setDefaultRequestProperties(headers);

                DefaultMediaSourceFactory factory =
                        new DefaultMediaSourceFactory(
                                DriveSingleFileCacheHelper.wrap(
                                        context,
                                        media.fileId,
                                        http
                                )
                        );

                String url =
                        "https://www.googleapis.com/drive/v3/files/"
                                + media.fileId + "?alt=media";

                MediaItem item = MediaItem.fromUri(url);

                mainHandler.post(() -> {
                    preparing.set(false);
                    if (!released)
                        callback.onReady(factory, item);
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    preparing.set(false);
                    if (!released)
                        callback.onError(e);
                });
            }
        });
    }

    @Override
    public void release() {
        released = true;
        cachedToken = null; // explicit safety
        executor.shutdownNow();
    }
}
