package com.github.jaykkumar01.vaultspace.media.datasource;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.TransferListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@UnstableApi
public final class DriveHttpDataSource implements DataSource {

    /* ---------------- CORE ---------------- */

    private final DriveStreamSource source;

    /* ---------------- SESSION ---------------- */

    private DriveStreamSource.StreamSession session;
    private InputStream stream;

    /* ---------------- STATE ---------------- */

    private @Nullable Uri uri;

    public DriveHttpDataSource(@NonNull Context ctx, @NonNull String fileId) {
        this.source = new DriveOkHttpStreamSource(ctx, fileId);
    }

    /* ========================= OPEN ========================= */

    @Override
    public long open(@NonNull DataSpec spec) throws IOException {

        uri = spec.uri;

        session = source.open(spec.position);
        stream = session.stream();

        long length = session.length();
        return length >= 0 ? length : C.LENGTH_UNSET;
    }

    /* ========================= READ ========================= */

    @Override
    public int read(@NonNull byte[] buffer, int offset, int length) throws IOException {

        if (stream == null)
            return C.RESULT_END_OF_INPUT;

        int read = stream.read(buffer, offset, length);

        if (read == -1)
            return C.RESULT_END_OF_INPUT;

        return read;
    }

    /* ========================= CLOSE ========================= */

    @Override
    public void close() {

        try { if (session != null) session.cancel(); }
        catch (Exception ignored) {}

        session = null;
        stream = null;
        uri = null;
    }

    /* ========================= MISC ========================= */

    @Override public void addTransferListener(@NonNull TransferListener listener) {}
    @Override public @Nullable Uri getUri() { return uri; }
    @Override public @NonNull Map<String, List<String>> getResponseHeaders() {
        return Collections.emptyMap();
    }


}
