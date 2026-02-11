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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

@UnstableApi
public final class DriveDataSource implements DataSource {

    private static final String TAG = "Drive:HybridStream";
    private static final int BUFFER_SIZE = 8 * 1024 * 1024;
    private static final int TEMP_READ = 128 * 1024;

    private final DriveStreamSource source;
    private final PrefetchPolicy policy;
    private final CircularBuffer buffer;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "Drive-Reader");
                t.setDaemon(true);
                return t;
            });

    private final AtomicLong generation = new AtomicLong();

    private volatile DriveStreamSource.StreamSession session;
    private volatile Future<?> readerTask;
    private @Nullable Uri uri;

    private long producedBytes, consumedBytes, openPosition, activeGen;

    public DriveDataSource(Context ctx, AlbumMedia media) {
        this.source = new DriveHttpSource(ctx, media.fileId);
        this.policy = new PrefetchPolicy(media);
        this.buffer = new CircularBuffer(BUFFER_SIZE);

        Log.d(TAG, "INIT fileId=" + media.fileId +
                " bitrate=" + policy.getAverageBitrateBytesPerSec() +
                " prefetch=" + policy.getBasePrefetchLimitBytes());
    }

    @Override
    public long open(DataSpec spec) throws IOException {

        invalidate();

        long myGen = generation.incrementAndGet();
        activeGen = myGen;
        openPosition = spec.position;
        producedBytes = 0;
        consumedBytes = 0;

        uri = spec.uri;

        buffer.setPrefetchLimit(policy.getBasePrefetchLimitBytes());

        session = source.open(spec.position);

        readerTask = executor.submit(() -> readerLoop(myGen));

        return C.LENGTH_UNSET;
    }

    private void readerLoop(long myGen) {
        byte[] temp = new byte[TEMP_READ];

        try (InputStream in = session.stream()) {
            int r;
            while (myGen == generation.get() && (r = in.read(temp)) != -1) {
                buffer.write(temp, r);
                producedBytes += r;
            }
        } catch (Exception ignored) {
        } finally {
            session.cancel();
            buffer.signalEof();
        }
    }

    @Override
    public int read(@NonNull byte[] target, int offset, int length) {
        try {
            int r = buffer.read(target, offset, length);
            if (r > 0) consumedBytes += r;
            return r == -1 ? C.RESULT_END_OF_INPUT : r;
        } catch (InterruptedException e) {
            return C.RESULT_END_OF_INPUT;
        }
    }

    private void invalidate() {
        if (activeGen != 0)
            Log.d(TAG, "CLOSE gen=" + activeGen +
                    " open@" + openPosition +
                    " wasted=" + (producedBytes - consumedBytes));

        generation.incrementAndGet();
        buffer.reset();

        if (readerTask != null) readerTask.cancel(true);
        if (session != null) session.cancel();

        readerTask = null;
        session = null;
        activeGen = 0;
    }

    @Override
    public void close() {
        invalidate();
        uri = null;
    }

    @Override
    public void addTransferListener(@NonNull TransferListener l) {
    }

    @Override
    public @Nullable Uri getUri() {
        return uri;
    }

    @Override
    public @NonNull Map<String, List<String>> getResponseHeaders() {
        return Collections.emptyMap();
    }

    public void release() {
    }
}
