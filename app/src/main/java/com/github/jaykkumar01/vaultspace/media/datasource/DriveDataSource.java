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

    /* ========================= CONSTANTS ========================= */

    private static final String TAG = "DriveDataSource";

    /* ========================= CORE ========================= */

    private final DriveStreamSource source;
    private final AlbumMedia media;

    /* ========================= SESSION ========================= */

    private DriveStreamSource.StreamSession session;
    private InputStream stream;

    /* ========================= STATE ========================= */

    private @Nullable Uri uri;
    private long openPosition;
    private boolean opened;

    /* ========================= READY METRICS ========================= */

    private long firstOpenStartNs;
    private boolean firstOpenLogged;

    private long lastOpenPosition;
    private long bytesReadInCycle;

    private long tailOpenPosition = -1;

    /* ========================= CONSTRUCTOR ========================= */

    public DriveDataSource(Context context, AlbumMedia media) {
        this.media = media;
        this.source = new DriveOkHttpSource(context, media.fileId);
        Log.d(TAG, "INIT fileId=" + media.fileId + " size=" + media.sizeBytes);
    }

    /* ========================= OPEN ========================= */

    @Override
    public long open(DataSpec spec) throws IOException {

        uri = spec.uri;
        openPosition = spec.position;
        lastOpenPosition = openPosition;
        bytesReadInCycle = 0;

        if (!firstOpenLogged) {
            firstOpenStartNs = System.nanoTime();
            firstOpenLogged = true;
        }

        // Detect tail probe
        if (media.sizeBytes > 0 && openPosition > 0 &&
                openPosition > media.sizeBytes / 2) {
            tailOpenPosition = openPosition;
        }

        Log.d(TAG, "OPEN @" + openPosition + " fileId=" + media.fileId);

        session = source.open(openPosition);
        stream = session.stream();
        opened = true;

        long length = session.length();
        return length >= 0 ? length : C.LENGTH_UNSET;
    }

    /* ========================= READ ========================= */

    @Override
    public int read(@NonNull byte[] target, int offset, int length) throws IOException {

        if (!opened || stream == null)
            return C.RESULT_END_OF_INPUT;

        int bytesRead = stream.read(target, offset, length);

        if (bytesRead == -1)
            return C.RESULT_END_OF_INPUT;

        bytesReadInCycle += bytesRead;
        return bytesRead;
    }

    /* ========================= CLOSE ========================= */

    @Override
    public void close() {

        if (!opened)
            return;

        try {
            if (stream != null)
                stream.close();
        } catch (Exception ignored) {}

        try {
            if (session != null)
                session.cancel();
        } catch (Exception ignored) {}

        stream = null;
        session = null;
        uri = null;
        opened = false;

        Log.d(TAG, "CLOSE @" + openPosition + " fileId=" + media.fileId);
    }

    /* ========================= PLAYER READY ========================= */

    public void onPlayerReady() {

        if (!firstOpenLogged)
            return;

        long readyMs = (System.nanoTime() - firstOpenStartNs) / 1_000_000;

        long headRequiredBytes = lastOpenPosition + bytesReadInCycle;

        long tailProbeBytes = 0;
        if (tailOpenPosition > 0 && media.sizeBytes > 0) {
            tailProbeBytes = media.sizeBytes - tailOpenPosition;
        }

        Log.d(TAG,
                "PLAYER READY fileId=" + media.fileId +
                        " timeToReadyMs=" + readyMs +
                        " headRequiredBytes=" + headRequiredBytes +
                        " tailProbeBytes=" + tailProbeBytes +
                        " fileSize=" + media.sizeBytes
        );
    }

    public void onPlayerRelease() {
        firstOpenLogged = false;
        firstOpenStartNs = 0L;
        bytesReadInCycle = 0;
        lastOpenPosition = 0;
        tailOpenPosition = -1;
    }

    /* ========================= MISC ========================= */

    @Override public void addTransferListener(@NonNull TransferListener listener) {}
    @Override public @Nullable Uri getUri() { return uri; }
    @Override public @NonNull Map<String, List<String>> getResponseHeaders() { return Collections.emptyMap(); }
}
