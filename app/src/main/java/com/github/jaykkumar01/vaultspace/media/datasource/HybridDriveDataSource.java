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

    private static final String TAG = "HybridChunk";
    private static final int START_CHUNK = 1024 * 1024;
    private static final int NORMAL_CHUNK = 1024 * 1024; // 1MB
    private final DriveSdkSource sdkSource;
    private final DriveHttpSource httpSource;
    private final long sizeBytes;

    private long currentPosition = C.INDEX_UNSET;
    private byte[] chunkBuffer;
    private int chunkOffset;
    private int chunkLength;

    private @Nullable Uri uri;

    public HybridDriveDataSource(Context context,String fileId,long sizeBytes) {
        this.sdkSource = new DriveSdkSource(context,fileId);
        this.httpSource = new DriveHttpSource(context,fileId);
        this.sizeBytes = sizeBytes;
    }

    @Override
    public void addTransferListener(@NonNull TransferListener listener) {}

    @Override
    public long open(DataSpec spec) throws IOException {
        close();
        uri = spec.uri;
        currentPosition = spec.position;

        Log.d(TAG,"open @" + currentPosition + " size=" + sizeBytes);

        if (sizeBytes > 0 && currentPosition >= sizeBytes) {
            Log.d(TAG,"open beyond EOF");
            return C.LENGTH_UNSET;
        }

        loadChunk(currentPosition);
        return C.LENGTH_UNSET;
    }

    private void loadChunk(long position) throws IOException {

        if (sizeBytes > 0 && position >= sizeBytes) {
            Log.d(TAG,"EOF reached @" + position);
            chunkBuffer = null;
            chunkLength = 0;
            chunkOffset = 0;
            return;
        }

        int chunkSize = position == 0 ? START_CHUNK : NORMAL_CHUNK;
        DriveSource source = position == 0 ? sdkSource : httpSource;

        chunkBuffer = source.fetchRange(position,chunkSize);
        chunkLength = chunkBuffer.length;
        chunkOffset = 0;

        Log.d(TAG,
                "load @" + position +
                        " requested=" + chunkSize +
                        " actual=" + chunkLength
        );
    }

    @Override
    public int read(@NonNull byte[] buffer,int offset,int length) throws IOException {

        if (chunkBuffer == null) return C.RESULT_END_OF_INPUT;

        if (chunkOffset >= chunkLength) {
            currentPosition += chunkLength;

            if (sizeBytes > 0 && currentPosition >= sizeBytes) {
                Log.d(TAG,"read EOF @" + currentPosition);
                return C.RESULT_END_OF_INPUT;
            }

            loadChunk(currentPosition);
            if (chunkLength == 0) return C.RESULT_END_OF_INPUT;
        }

        int toCopy = Math.min(length,chunkLength - chunkOffset);
        System.arraycopy(chunkBuffer,chunkOffset,buffer,offset,toCopy);
        chunkOffset += toCopy;

        return toCopy;
    }

    @Override
    public @Nullable Uri getUri() {
        return uri;
    }

    @NonNull
    @Override
    public Map<String,List<String>> getResponseHeaders() {
        return Collections.emptyMap();
    }

    @Override
    public void close() {
        if (currentPosition != C.INDEX_UNSET)
            Log.d(TAG,"close @" + currentPosition);

        chunkBuffer = null;
        chunkOffset = 0;
        chunkLength = 0;
        currentPosition = C.INDEX_UNSET;
        uri = null;
    }
}
