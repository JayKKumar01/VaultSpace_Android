package com.github.jaykkumar01.vaultspace.media.datasource;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.TransferListener;

import com.google.api.services.drive.Drive;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public final class DriveSdkDataSource implements DataSource {

    private static final String TAG = "Video:DriveSdkDS";

    private final Drive drive;
    private final String fileId;

    private @Nullable InputStream stream;
    private @Nullable Uri uri;
    private long currentPosition;

    public DriveSdkDataSource(
            @NonNull Drive drive,
            @NonNull String fileId
    ) {
        this.drive = drive;
        this.fileId = fileId;
    }

    @Override
    public void addTransferListener(@NonNull TransferListener transferListener) {}

    @Override
    public long open(@NonNull DataSpec spec) throws IOException {

        uri = spec.uri;
        long targetPos = spec.position;

        Log.d(TAG, "[" + fileId + "] open @" + targetPos);

        closeQuietly();
        stream = openFreshStream();
        currentPosition = 0;

        if (targetPos > 0) {
            Log.d(TAG, "[" + fileId + "] skip " + targetPos);
            skipFully(stream, targetPos);
            currentPosition = targetPos;
        }

        return C.LENGTH_UNSET;
    }

    @Override
    public int read(
            @NonNull byte[] buffer,
            int offset,
            int length
    ) throws IOException {

        if (stream == null)
            return C.RESULT_END_OF_INPUT;

        int read = stream.read(buffer, offset, length);
        if (read == -1)
            return C.RESULT_END_OF_INPUT;

        currentPosition += read;
        return read;
    }

    @Override
    public @Nullable Uri getUri() {
        return uri;
    }

    @NonNull
    @Override
    public Map<String, List<String>> getResponseHeaders() {
        return DataSource.super.getResponseHeaders();
    }

    @Override
    public void close() {
        Log.d(TAG, "[" + fileId + "] close @" + currentPosition);
        closeQuietly();
        uri = null;
    }

    /* ---------------- internals ---------------- */

    private InputStream openFreshStream() throws IOException {
        Log.d(TAG, "[" + fileId + "] open fresh stream");
        return drive.files()
                .get(fileId)
                .executeMediaAsInputStream();
    }

    private static void skipFully(InputStream in, long bytes) throws IOException {
        long remaining = bytes;
        while (remaining > 0) {
            long skipped = in.skip(remaining);
            if (skipped <= 0)
                throw new EOFException("Failed skip " + bytes);
            remaining -= skipped;
        }
    }

    private void closeQuietly() {
        if (stream != null) {
            try { stream.close(); } catch (Exception ignored) {}
            stream = null;
        }
    }
}
