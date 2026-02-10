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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@UnstableApi
public final class HybridDriveDataSource implements DataSource {

    private static final String TAG = "Video:DriveDS";

    private final DriveSdkSource sdkSource;
    private final DriveHttpSource httpSource;

    private @Nullable Uri uri;
    private @Nullable DriveSource activeSource;

    public HybridDriveDataSource(Context context, String fileId) {
        this.sdkSource = new DriveSdkSource(context, fileId);
        this.httpSource = new DriveHttpSource(context, fileId);
    }

    @Override
    public void addTransferListener(@NonNull TransferListener transferListener) {}

    @Override
    public long open(DataSpec spec) throws IOException {
        close();
        uri = spec.uri;

        boolean sdk = spec.position == 0;
        Log.d(TAG, "[" + (sdk ? "SDK" : "HTTP") + "] selected");

        activeSource = sdk ? sdkSource : httpSource;
        return activeSource.open(spec);
    }

    @Override
    public int read(@NonNull byte[] buffer, int offset, int length)
            throws IOException {

        return activeSource == null
                ? C.RESULT_END_OF_INPUT
                : activeSource.read(buffer, offset, length);
    }

    @Override
    public @Nullable Uri getUri() {
        return uri;
    }

    @NonNull
    @Override
    public Map<String, List<String>> getResponseHeaders() {
        return activeSource == null
                ? Collections.emptyMap()
                : activeSource.getResponseHeaders();
    }

    @Override
    public void close() {
        if (activeSource != null) {
            Log.d(TAG, "[close] active source");
            activeSource.close();
            activeSource = null;
        }
        uri = null;
    }
}
