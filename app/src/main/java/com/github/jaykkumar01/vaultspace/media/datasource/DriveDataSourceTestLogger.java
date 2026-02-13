package com.github.jaykkumar01.vaultspace.media.datasource;

import android.util.Log;

final class DriveDataSourceTestLogger {

    private static final String TAG = "DriveDS:Test";

    private long openStartNs;
    private long sessionStartNs;

    private long producedBytes;
    private long consumedBytes;

    private long producerWaitNs;
    private long consumerWaitNs;

    /* ========================= OPEN ========================= */

    void onOpenStart(String fileId, long position) {
        openStartNs = System.nanoTime();
        Log.d(TAG, "OPEN start @" + position + ", fileId: "+fileId);
    }

    void onOpenComplete() {
        long openMs = (System.nanoTime() - openStartNs) / 1_000_000;
        sessionStartNs = System.nanoTime();
        Log.d(TAG, "OPEN complete in " + openMs + " ms");
    }

    /* ========================= PRODUCER ========================= */

    void onProduced(int bytes) {
        producedBytes += bytes;
    }

    void onProducerWaitStart() {
        producerWaitNs -= System.nanoTime();
    }

    void onProducerWaitEnd() {
        producerWaitNs += System.nanoTime();
    }

    /* ========================= CONSUMER ========================= */

    void onConsumed(int bytes) {
        consumedBytes += bytes;
    }

    void onConsumerWaitStart() {
        consumerWaitNs -= System.nanoTime();
    }

    void onConsumerWaitEnd() {
        consumerWaitNs += System.nanoTime();
    }

    /* ========================= CLOSE ========================= */

    void onSessionClose(long openPosition) {
        long durationMs = (System.nanoTime() - sessionStartNs) / 1_000_000;

        long producerWaitMs = producerWaitNs / 1_000_000;
        long consumerWaitMs = consumerWaitNs / 1_000_000;

        long throughputKBps =
                durationMs == 0 ? 0 :
                (consumedBytes / 1024) * 1000 / durationMs;

        Log.d(TAG,
                "\n========= DRIVE SESSION =========" +
                "\nopen@" + openPosition +
                "\ndurationMs=" + durationMs +
                "\nproduced=" + producedBytes +
                "\nconsumed=" + consumedBytes +
                "\nproducerWaitMs=" + producerWaitMs +
                "\nconsumerWaitMs=" + consumerWaitMs +
                "\nthroughputKBps=" + throughputKBps +
                "\n================================="
        );
    }
}
