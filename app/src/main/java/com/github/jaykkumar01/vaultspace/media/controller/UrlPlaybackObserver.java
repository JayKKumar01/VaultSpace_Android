package com.github.jaykkumar01.vaultspace.media.controller;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.media3.common.C;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class UrlPlaybackObserver {

    private static final String TAG = "UrlPlaybackObserver";

    public interface DecisionCallback {
        void onHealthy();

        void onUnhealthy();
    }

    /* ---------------- tuning ---------------- */

    private static final long START_TIMEOUT_MS = 20_000;
    private static final long BASE_OVERALL_MS = 3_000;
    private static final long PER_BUCKET_MS = 1_000;
    private static final long SIZE_BUCKET = 10L * 1024 * 1024; // 10 MB
    private static final long MAX_OVERALL_MS = 20_000;

    private static final int MIN_TOTAL_BYTES = 128 * 1024; // 128 KB

    /* ---------------- deps ---------------- */

    private final Handler main = new Handler(Looper.getMainLooper());
    private final DecisionCallback callback;
    private final AtomicBoolean decided = new AtomicBoolean(false);

    /* ---------------- identity ---------------- */

    private final String fileId;
    private final long overallTimeoutMs;

    /* ---------------- timing ---------------- */

    private long healthStartMs = -1;

    /* ---------------- runtime ---------------- */

    private boolean started;
    private int initCount;
    private final AtomicInteger totalBytes = new AtomicInteger(0);

    /**
     * Guards START timeout scheduling
     */
    private final AtomicBoolean startTimeoutArmed = new AtomicBoolean(false);

    public UrlPlaybackObserver(
            @NonNull AlbumMedia media,
            @NonNull DecisionCallback callback
    ) {
        this.fileId = media.fileId;
        this.callback = callback;
//        this.overallTimeoutMs = computeOverallTimeout(media.sizeBytes);
        this.overallTimeoutMs = START_TIMEOUT_MS;
    }

    /* ---------------- lifecycle ---------------- */

    public void start() {
        healthStartMs = SystemClock.uptimeMillis();

        Log.d(
                TAG,
                "[" + fileId + "] health window started"
                        + " (overallTimeout=" + overallTimeoutMs + " ms)"
        );

        // Overall timeout starts immediately (unchanged)
        main.postDelayed(this::checkOverallTimeout, overallTimeoutMs);
    }

    public void cancel() {
        main.removeCallbacksAndMessages(null);
        decided.set(true);
    }

    /* ---------------- cache success ---------------- */

    public void onPlayerReady() {
        if (decided.compareAndSet(false, true)) {
            logHealthy("Player READY (cache)");
            callback.onHealthy();
        }
    }

    /* ---------------- transfer events ---------------- */

    public void onInit() {
        if (decided.get()) return;

        initCount++;
        long elapsed = SystemClock.uptimeMillis() - healthStartMs;

        Log.d(TAG, "[" + fileId + "] INIT #" + initCount + " (+" + elapsed + " ms)");

        // 🔑 START timeout must be armed ONCE, on first INIT only
        if (startTimeoutArmed.compareAndSet(false, true)) {
            main.postDelayed(this::checkStartTimeout, START_TIMEOUT_MS);
        }
    }

    public void onStart() {
        if (decided.get()) return;

        started = true;
        Log.d(TAG, "[" + fileId + "] START received");
    }

    public void onData(int bytes) {
        if (decided.get() || !started || bytes <= 0) return;

        if (totalBytes.addAndGet(bytes) >= MIN_TOTAL_BYTES) {
            decideHealthy();
        }
    }

    public void onPositionRewind() {
        if (decided.get()) return;
        Log.d(TAG, "[" + fileId + "] POSITION REWIND detected (normal)");
    }

    public void onRangeRequest(long position, long length) {
        if (decided.get()) return;

        String range;
        if (length == C.LENGTH_UNSET) {
            range = "bytes=" + position + "-";
        } else {
            range = "bytes=" + position + "-" + (position + length - 1);
        }

        Log.d(TAG, "[" + fileId + "] HTTP " + range);
    }



    /* ---------------- rules ---------------- */

    private void checkStartTimeout() {
        if (decided.get()) return;

        if (!started) {
            Log.w(TAG, "[" + fileId + "] START timeout → unhealthy");
            decideUnhealthy();
        }
    }

    private void checkOverallTimeout() {
        if (decided.get()) return;

        int total = totalBytes.get();
        if (total < MIN_TOTAL_BYTES) {
            Log.w(
                    TAG,
                    "[" + fileId + "] OVERALL timeout (" +
                            total + " bytes) → unhealthy"
            );
            decideUnhealthy();
        }
    }

    /* ---------------- helpers ---------------- */

    private long computeOverallTimeout(long sizeBytes) {
        long buckets = sizeBytes / SIZE_BUCKET;
        long timeout = BASE_OVERALL_MS + buckets * PER_BUCKET_MS;
        return Math.min(timeout, MAX_OVERALL_MS);
    }

    private void decideHealthy() {
        if (decided.compareAndSet(false, true)) {
            logHealthy("data threshold");
            callback.onHealthy();
        }
    }

    private void decideUnhealthy() {
        if (decided.compareAndSet(false, true)) {
            Log.i(TAG, "[" + fileId + "] DECISION = UNHEALTHY");
            callback.onUnhealthy();
        }
    }

    private void logHealthy(String reason) {
        long elapsed = SystemClock.uptimeMillis() - healthStartMs;
        Log.i(
                TAG,
                "[" + fileId + "] DECISION = HEALTHY ("
                        + reason + ", "
                        + elapsed + " ms, "
                        + totalBytes.get() + " bytes)"
        );
    }
}
