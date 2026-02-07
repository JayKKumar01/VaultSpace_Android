package com.github.jaykkumar01.vaultspace.media.helper;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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

    /**
     * Lightweight refresh interval (safe, cheap)
     */
    private static final long REFRESH_INTERVAL_MS = 5 * 60 * 1000L; // 5 min

    private final GoogleAccountCredential credential;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final Handler refreshHandler;

    /**
     * Shared mutable headers (ExoPlayer reads this on every request)
     */
    private final Map<String, String> headers = new HashMap<>();

    public VideoMediaDriveHelper(@NonNull Context context) {
        this.credential = GoogleCredentialFactory.forPrimaryDrive(context);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.refreshHandler = new Handler(Looper.getMainLooper());
    }

    /* ---------------- public API ---------------- */

    @OptIn(markerClass = UnstableApi.class)
    public void prepare(@NonNull AlbumMedia media,
                        @NonNull Callback cb) {

        executor.execute(() -> {
            try {
                // Initial token (blocking, background thread)
                String token = credential.getToken();
                if (token == null || token.isEmpty())
                    throw new IllegalStateException("Drive token missing");

                headers.put("Authorization", "Bearer " + token);


                DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
                        .setAllowCrossProtocolRedirects(true)
                        .setDefaultRequestProperties(headers);

                DefaultMediaSourceFactory factory = new DefaultMediaSourceFactory(DriveSingleFileCacheHelper.wrap(
                        credential.getContext(), media.fileId, http));


                String url =
                        "https://www.googleapis.com/drive/v3/files/"
                                + media.fileId + "?alt=media";

                startPeriodicRefresh();

                mainHandler.post(() -> cb.onReady(factory, url));

            } catch (Exception e) {
                mainHandler.post(() -> cb.onError(e));
            }
        });
    }

    public void release() {
        refreshHandler.removeCallbacksAndMessages(null);
        executor.shutdownNow();
    }

    /* ---------------- token refresh ---------------- */

    private void startPeriodicRefresh() {
        refreshHandler.removeCallbacksAndMessages(null);
        refreshHandler.postDelayed(this::refreshToken, REFRESH_INTERVAL_MS);
    }

    private void refreshToken() {
        executor.execute(() -> {
            try {
                String token = credential.getToken();
                if (token != null && !token.isEmpty()) {
                    headers.put("Authorization", "Bearer " + token);
                    Log.d(TAG, "Drive token refreshed (periodic)");
                }
            } catch (Exception e) {
                Log.w(TAG, "Drive token refresh failed (will retry)", e);
            } finally {
                // Always reschedule (fail-soft)
                startPeriodicRefresh();
            }
        });
    }
}
