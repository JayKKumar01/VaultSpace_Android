package com.github.jaykkumar01.vaultspace.media.listener;

import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.TransferListener;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@UnstableApi
public final class MediaTransferTraceListener implements TransferListener {

    private static final String TAG = "Video:MediaDS";

    /* ---------------- core ---------------- */

    private final String fileId; private final long sizeBytes;

    private final AtomicLong openTimeMs = new AtomicLong();
    private final AtomicLong bytesRead = new AtomicLong();
    private final AtomicBoolean firstReadLogged = new AtomicBoolean();

    private volatile long openPosition = -1;

    public MediaTransferTraceListener(@NonNull AlbumMedia media) {
        this.fileId = media.fileId;
        this.sizeBytes = media.sizeBytes;
    }

    /* ---------------- open (REAL START) ---------------- */

    @Override
    public void onTransferInitializing(@NonNull DataSource source,@NonNull DataSpec dataSpec,boolean isNetwork) {
        openPosition = dataSpec.position;
        openTimeMs.set(SystemClock.elapsedRealtime());
        bytesRead.set(0);
        firstReadLogged.set(false);

        Log.d(TAG, "[" + fileId + "] open → HTTP @" + openPosition + " size=" + sizeBytes);
    }

    @Override
    public void onTransferStart(@NonNull DataSource source,@NonNull DataSpec dataSpec,boolean isNetwork) {
        // intentionally ignored (fires too late for latency insights)
    }

    /* ---------------- read ---------------- */

    @Override
    public void onBytesTransferred(@NonNull DataSource source,@NonNull DataSpec dataSpec,boolean isNetwork,int bytes) {
        if (bytes <= 0) return;

        bytesRead.addAndGet(bytes);

        if (firstReadLogged.compareAndSet(false, true)) {
            long gapMs = SystemClock.elapsedRealtime() - openTimeMs.get();
            Log.d(TAG, "[" + fileId + "] first read +" + gapMs + "ms @" + openPosition);
        }
    }

    /* ---------------- close ---------------- */

    @Override
    public void onTransferEnd(@NonNull DataSource source,@NonNull DataSpec dataSpec,boolean isNetwork) {
        long total = bytesRead.get();
        long closedAt = openPosition >= 0 ? openPosition + total : -1;

        Log.d(TAG, "[" + fileId + "] close @" + closedAt +
                " read=" + total +
                " of " + sizeBytes);

        openPosition = -1;
        openTimeMs.set(0);
        bytesRead.set(0);
        firstReadLogged.set(false);
    }
}
