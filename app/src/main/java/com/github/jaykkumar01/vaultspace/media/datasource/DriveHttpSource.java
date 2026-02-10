package com.github.jaykkumar01.vaultspace.media.datasource;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.media3.common.C;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.HttpDataSource;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class DriveHttpSource implements DriveSource {

    private static final String TAG = "Video:DriveDS";

    private final String fileId;
    private final DriveAuthGate authGate;

    private HttpDataSource source;

    DriveHttpSource(Context context, String fileId) {
        this.fileId = fileId;
        this.authGate = DriveAuthGate.get(context);
    }

    @Override
    public long open(DataSpec spec) throws IOException {
        Log.d(TAG, "[" + fileId + "] HTTP open @" + spec.position);

        try {
            return openInternal(spec, authGate.requireToken());
        } catch (IOException first) {
            Log.w(TAG, "[" + fileId + "] HTTP failed, retrying with fresh token");
            return openInternal(spec, authGate.refreshTokenAfterFailure());
        }
    }

    private long openInternal(DataSpec spec, String token) throws IOException {
        DefaultHttpDataSource.Factory factory =
                new DefaultHttpDataSource.Factory()
                        .setDefaultRequestProperties(Map.of(
                                "Authorization", "Bearer " + token,
                                "Range", "bytes=" + spec.position + "-"
                        ));

        source = factory.createDataSource();

        Uri uri = Uri.parse(
                "https://www.googleapis.com/drive/v3/files/"
                        + fileId + "?alt=media"
        );

        source.open(new DataSpec(uri, spec.position, C.LENGTH_UNSET));
        return C.LENGTH_UNSET;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (source == null) return C.RESULT_END_OF_INPUT;
        int read = source.read(buffer, offset, length);
        return read == -1 ? C.RESULT_END_OF_INPUT : read;
    }

    @Override
    public Map<String, List<String>> getResponseHeaders() {
        return source != null ? source.getResponseHeaders() : Collections.emptyMap();
    }

    @Override
    public void close() {
        Log.d(TAG, "[" + fileId + "] HTTP close");
        if (source != null) {
            try { source.close(); } catch (Exception ignored) {}
            source = null;
        }
    }
}
