package com.github.jaykkumar01.vaultspace.media.datasource;

import android.content.Context;
import android.util.Log;

import androidx.media3.common.C;
import androidx.media3.datasource.DataSpec;

import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.google.api.services.drive.Drive;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class DriveSdkSource implements DriveSource {

    private static final String TAG = "Video:DriveDS";

    private final Drive drive;
    private final String fileId;

    private InputStream stream;

    DriveSdkSource(Context context, String fileId) {
        this.drive = DriveClientProvider.getPrimaryDrive(context);;
        this.fileId = fileId;
    }

    @Override
    public long open(DataSpec spec) throws IOException {
        Log.d(TAG, "[" + fileId + "] SDK open @0");
        stream = drive.files().get(fileId).executeMediaAsInputStream();
        return C.LENGTH_UNSET;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (stream == null) return C.RESULT_END_OF_INPUT;
        int read = stream.read(buffer, offset, length);
        return read == -1 ? C.RESULT_END_OF_INPUT : read;
    }

    @Override
    public Map<String, List<String>> getResponseHeaders() {
        return Collections.emptyMap();
    }

    @Override
    public void close() {
        Log.d(TAG, "[" + fileId + "] SDK close");
        if (stream != null) {
            try { stream.close(); } catch (Exception ignored) {}
            stream = null;
        }
    }
}
