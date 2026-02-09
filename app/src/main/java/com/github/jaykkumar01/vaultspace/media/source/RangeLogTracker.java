package com.github.jaykkumar01.vaultspace.media.source;

/* ---------------- range logging helper ---------------- */

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.TransferListener;

import com.github.jaykkumar01.vaultspace.media.base.RangeDecisionCallback;

@OptIn(markerClass = UnstableApi.class)
public final class RangeLogTracker implements TransferListener {

    private static final String TAG = "DriveUrlRanges";

    private final String fileId;
    private final RangeDecisionCallback callback;

    /* ---------------- tracking state ---------------- */

    private int requestCount;
    private int resetCount;

    private long lastPosition = -1;
    private long lastLength = -1;

    private long lastInitAtMs;
    private long bytesSinceLastLog;
    private long lastBytesLogMs;

    /* ---------------- constructor ---------------- */

    public RangeLogTracker(@NonNull String fileId,
                           @NonNull RangeDecisionCallback callback) {
        this.fileId = fileId;
        this.callback = callback;
    }

    /* ---------------- TransferListener ---------------- */

    @Override
    public void onTransferInitializing(@NonNull DataSource source,
                                       @NonNull DataSpec spec,
                                       boolean isNetwork) {

        lastInitAtMs = System.currentTimeMillis();

        Log.d(TAG,
                "[" + fileId + "] INIT  pos=" + spec.position +
                        " len=" + spec.length +
                        " flags=" + spec.flags +
                        " network=" + isNetwork);

        callback.onInit(fileId, spec);
    }

    @Override
    public void onTransferStart(@NonNull DataSource source,
                                @NonNull DataSpec spec,
                                boolean isNetwork) {

        requestCount++;

        long now = System.currentTimeMillis();
        long initToStartMs = lastInitAtMs > 0 ? now - lastInitAtMs : -1;

        boolean isReset = lastPosition != -1 && spec.position < lastPosition;
        boolean isRepeat = spec.position == lastPosition && spec.length == lastLength;

        if (isReset) resetCount++;

        Log.d(TAG,
                "[" + fileId + "] " +
                        buildStartLog(spec, isNetwork, isReset, isRepeat) +
                        " init→start=" + initToStartMs + "ms");

        callback.onStart(fileId, spec, initToStartMs);

        lastPosition = spec.position;
        lastLength = spec.length;
        bytesSinceLastLog = 0;
        lastBytesLogMs = now;
    }

    @Override
    public void onBytesTransferred(@NonNull DataSource source,
                                   @NonNull DataSpec spec,
                                   boolean isNetwork,
                                   int bytesTransferred) {

        bytesSinceLastLog += bytesTransferred;

        long now = System.currentTimeMillis();
        if (now - lastBytesLogMs < 500) return;

        Log.d(TAG,
                "[" + fileId + "] DATA  pos=" + spec.position +
                        " +" + formatBytes(bytesSinceLastLog) +
                        " network=" + isNetwork);

        bytesSinceLastLog = 0;
        lastBytesLogMs = now;
    }

    @Override
    public void onTransferEnd(@NonNull DataSource source,
                              @NonNull DataSpec spec,
                              boolean isNetwork) {

        if (bytesSinceLastLog > 0) {
            Log.d(TAG,
                    "[" + fileId + "] DATA  pos=" + spec.position +
                            " +" + formatBytes(bytesSinceLastLog) +
                            " network=" + isNetwork);
            bytesSinceLastLog = 0;
        }

        Log.d(TAG,
                "[" + fileId + "] END   pos=" + spec.position +
                        " len=" + spec.length +
                        " requests=" + requestCount +
                        " resets=" + resetCount);

        callback.onEnd(fileId, spec, requestCount, resetCount);
    }

    /* ---------------- callback contract ---------------- */

    /* ---------------- log helpers ---------------- */

    private static String buildStartLog(@NonNull DataSpec spec,
                                        boolean isNetwork,
                                        boolean isReset,
                                        boolean isRepeat) {

        StringBuilder sb = new StringBuilder(128);
        sb.append("START pos=").append(spec.position)
                .append(" len=").append(spec.length);

        if (spec.length == C.LENGTH_UNSET) sb.append(" (LEN_UNSET)");
        if (isReset) sb.append(" ⚠ RESET");
        else if (isRepeat) sb.append(" ↻ REPEAT");

        sb.append(" flags=").append(spec.flags)
                .append(" network=").append(isNetwork);

        return sb.toString();
    }

    @SuppressLint("DefaultLocale")
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        float kb = bytes / 1024f;
        if (kb < 1024) return String.format("%.1f KB", kb);
        return String.format("%.1f MB", kb / 1024f);
    }
}
