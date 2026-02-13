package com.github.jaykkumar01.vaultspace.media.datasource;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.TransferListener;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@UnstableApi
public final class DriveDataSource implements DataSource {

    private static final String TAG = "DriveDataSource";

    /* ---------------- CORE ---------------- */

    private final DriveStreamSource source;
    private final AlbumMedia media;

    /* ---------------- TIMING ---------------- */

    private long startNs = 0L;
    private boolean timerStarted = false;

    /* ---------------- SESSION ---------------- */

    private DriveStreamSource.StreamSession session;
    private InputStream stream;

    /* ---------------- STATE ---------------- */

    private @Nullable Uri uri;
    private long openPosition;
    private long bytesRead;

    public DriveDataSource(Context context, AlbumMedia media) {
        this.media = media;
        this.source = new DriveOkHttpSource(context, media.fileId);
        Log.d(TAG, "INIT fileId=" + media.fileId + " size=" + media.sizeBytes);
    }

    /* ========================= OPEN ========================= */

    @Override
    public long open(DataSpec spec) throws IOException {

        if (!timerStarted) {
            startNs = System.nanoTime();
            timerStarted = true;
        }

        uri = spec.uri;
        openPosition = spec.position;
        bytesRead = 0;

        Log.d(TAG, "OPEN @" + openPosition + " fileId=" + media.fileId);

        session = source.open(openPosition);
        stream = session.stream();

        long length = session.length();
        return length >= 0 ? length : C.LENGTH_UNSET;
    }

    /* ========================= READ ========================= */

    @Override
    public int read(@NonNull byte[] target, int offset, int length) throws IOException {

        if (stream == null)
            return C.RESULT_END_OF_INPUT;

        int read = stream.read(target, offset, length);

        if (read == -1)
            return C.RESULT_END_OF_INPUT;

        bytesRead += read;
        return read;
    }

    /* ========================= CLOSE ========================= */

    @Override
    public void close() {

        try { if (stream != null) stream.close(); } catch (Exception ignored) {}
        try { if (session != null) session.cancel(); } catch (Exception ignored) {}

        Log.d(TAG,
                "CLOSE @" + openPosition +
                        " fileId=" + media.fileId +
                        " bytesRead=" + bytesRead
        );

        stream = null;
        session = null;
        uri = null;
    }

    /* ========================= PREFETCH READY ========================= */

    /**
     * Call this right before player.prepare()
     * if you are using prefetch.
     */
    public void onPrefetchReady() {
        startNs = System.nanoTime();
        timerStarted = true;
    }

    /* ========================= PLAYER READY ========================= */

    public void onPlayerReady() {
        if (!timerStarted) return;

        long readyMs = (System.nanoTime() - startNs) / 1_000_000;
        Log.d(TAG, "PLAYER READY fileId=" + media.fileId + " timeToReadyMs=" + readyMs);
    }

    /* ========================= RELEASE ========================= */

    public void onPlayerRelease() {
        startNs = 0L;
        timerStarted = false;
    }

    /* ========================= MISC ========================= */

    @Override public void addTransferListener(@NonNull TransferListener listener) {}
    @Override public @Nullable Uri getUri() { return uri; }
    @Override public @NonNull Map<String, List<String>> getResponseHeaders() { return Collections.emptyMap(); }
}
