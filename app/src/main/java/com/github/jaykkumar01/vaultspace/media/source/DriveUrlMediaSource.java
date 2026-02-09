package com.github.jaykkumar01.vaultspace.media.source;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.TransferListener;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.core.auth.GoogleCredentialFactory;
import com.github.jaykkumar01.vaultspace.media.base.RangeDecisionCallback;
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
    private volatile String cachedToken; // in-memory Drive token
    private RangeDecisionCallback rangeDecisionCallback;

    /* ---------------- constructor ---------------- */

    public DriveUrlMediaSource(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.credential = GoogleCredentialFactory.forPrimaryDrive(this.context);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    public void setRangeDecisionCallback(RangeDecisionCallback rangeDecisionCallback){
        this.rangeDecisionCallback = rangeDecisionCallback;
    }

    /* ---------------- VideoMediaSource ---------------- */

    @Override
    public void prepare(@NonNull AlbumMedia media,
                        @NonNull VideoMediaPrepareCallback callback) {
        if (released || !preparing.compareAndSet(false, true)) return;

        executor.execute(() -> {
            try {
                if (released) return;

                String token = getOrFetchToken();
                Map<String, String> headers = new HashMap<>(1);
                headers.put("Authorization", "Bearer " + token);

                DefaultHttpDataSource.Factory http =
                        new DefaultHttpDataSource.Factory()
                                .setAllowCrossProtocolRedirects(true)
                                .setDefaultRequestProperties(headers)
                                .setTransferListener(new RangeLogTracker(media.fileId, rangeDecisionCallback));

                DefaultMediaSourceFactory factory =
                        new DefaultMediaSourceFactory(
                                DriveSingleFileCacheHelper.wrap(
                                        context,
                                        media.fileId,
                                        http
                                )
                        );

                MediaItem item = MediaItem.fromUri(
                        "https://www.googleapis.com/drive/v3/files/"
                                + media.fileId + "?alt=media"
                );

                mainHandler.post(() -> {
                    preparing.set(false);
                    if (!released) callback.onReady(factory, item);
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    preparing.set(false);
                    if (!released) callback.onError(e);
                });
            }
        });
    }

    @Override
    public void release() {
        released = true;
        cachedToken = null;
        executor.shutdownNow();
    }

    /* ---------------- internal helpers ---------------- */

    private String getOrFetchToken() throws Exception {
        if (cachedToken != null) return cachedToken;
        String token = credential.getToken();
        if (token == null || token.isEmpty())
            throw new IllegalStateException("Drive token missing");
        return cachedToken = token;
    }
}
