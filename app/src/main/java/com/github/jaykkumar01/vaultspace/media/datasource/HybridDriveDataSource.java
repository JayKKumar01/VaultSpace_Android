package com.github.jaykkumar01.vaultspace.media.datasource;

import android.content.Context;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.TransferListener;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@UnstableApi
public final class HybridDriveDataSource implements DataSource {

    private static final String TAG = "Video:DriveDS";

    private final DriveSdkSource sdkSource;
    private final DriveHttpSource httpSource;
    private final String fileId;

    private final AtomicLong bytesRead = new AtomicLong();
    private final AtomicLong openTimeMs = new AtomicLong();
    private final AtomicBoolean firstReadLogged = new AtomicBoolean();

    private volatile long openPosition = C.INDEX_UNSET;
    private volatile @Nullable Uri uri;
    private volatile @Nullable DriveSource activeSource;

    public HybridDriveDataSource(Context context, String fileId) {
        this.fileId = fileId;
        this.sdkSource = new DriveSdkSource(context, fileId);
        this.httpSource = new DriveHttpSource(context, fileId);
    }

    @Override
    public void addTransferListener(@NonNull TransferListener listener) {}

    @Override
    public long open(DataSpec spec) throws IOException {
        close();

        uri = spec.uri;
        openPosition = spec.position;
        bytesRead.set(0);
        openTimeMs.set(SystemClock.elapsedRealtime());
        firstReadLogged.set(false);

        boolean useSdk = spec.position == 0;
        DriveSource source = useSdk ? sdkSource : httpSource;
        activeSource = source;

        Log.d(TAG, "[" + fileId + "] open → " + (useSdk ? "SDK" : "HTTP") +
                " @" + spec.position);

        return source.open(spec);
    }

    @Override
    public int read(@NonNull byte[] buffer, int offset, int length)
            throws IOException {

        DriveSource source = activeSource;
        if (source == null) return C.RESULT_END_OF_INPUT;

        int read = source.read(buffer, offset, length);

        if (read > 0) {
            bytesRead.addAndGet(read);

            if (firstReadLogged.compareAndSet(false, true)) {
                long gapMs = SystemClock.elapsedRealtime() - openTimeMs.get();
                Log.d(TAG, "[" + fileId + "] first read +" + gapMs + "ms");
            }
        }

        return read;
    }

    @Override
    public @Nullable Uri getUri() {
        return uri;
    }

    @NonNull
    @Override
    public Map<String, List<String>> getResponseHeaders() {
        DriveSource source = activeSource;
        return source == null
                ? Collections.emptyMap()
                : source.getResponseHeaders();
    }

    @Override
    public void close() {
        DriveSource source = activeSource;
        activeSource = null;

        long closedAt = openPosition != C.INDEX_UNSET
                ? openPosition + bytesRead.get()
                : C.INDEX_UNSET;

        if (source != null) {
            Log.d(TAG, "[" + fileId + "] close @" + closedAt);
            try { source.close(); } catch (Exception ignored) {}
        }

        uri = null;
        openPosition = C.INDEX_UNSET;
        bytesRead.set(0);
        openTimeMs.set(0);
        firstReadLogged.set(false);
    }
}
