package com.github.jaykkumar01.vaultspace.album.upload;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.session.UploadRetryStore;
import com.github.jaykkumar01.vaultspace.core.session.cache.UploadCache;
import com.github.jaykkumar01.vaultspace.models.MediaSelection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class UploadManager {

    private static final String TAG = "VaultSpace:UploadMgr";

    private final UploadCache uploadCache;
    private final UploadRetryStore retryStore;

    private final Map<String, UploadObserver> observers = new HashMap<>();
    private final Deque<UploadTask> queue = new ArrayDeque<>();

    private UploadTask currentTask;
    private boolean stopped;

    private AlbumUploadOrchestrator orchestrator;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();


    public UploadManager(
            @NonNull UploadCache uploadCache,
            @NonNull UploadRetryStore retryStore
    ) {
        this.uploadCache = uploadCache;
        this.retryStore = retryStore;
    }

    /* ================= Wiring ================= */

    public void attachOrchestrator(
            @NonNull AlbumUploadOrchestrator orchestrator
    ) {
        this.orchestrator = orchestrator;
    }

    private void notifyNotification() {
        if (orchestrator != null) {
            orchestrator.onUploadStateChanged();
        }
    }

    /* ================= Observer ================= */

    public void registerObserver(
            @NonNull String albumId,
            @NonNull UploadObserver observer
    ) {
        observers.put(albumId, observer);

        UploadSnapshot snapshot = uploadCache.getSnapshot(albumId);
        if (snapshot != null) {
            observer.onSnapshot(snapshot);
        }
    }

    public void unregisterObserver(@NonNull String albumId) {
        observers.remove(albumId);
    }

    private void emitIfActive(
            @NonNull String albumId,
            @NonNull UploadSnapshot snapshot
    ) {
        UploadObserver observer = observers.get(albumId);
        if (observer != null) {
            observer.onSnapshot(snapshot);
        }
    }

    /* ================= Enqueue ================= */

    public void enqueue(
            @NonNull String albumId,
            @NonNull String albumName,
            @NonNull List<MediaSelection> selections
    ) {
        Log.d(TAG, "enqueue(): albumId=" + albumId + ", selections=" + selections.size());

        int photos = 0, videos = 0;
        for (MediaSelection s : selections) {
            if (s.isVideo) videos++;
            else photos++;
        }
        if (photos + videos == 0) {
            Log.w(TAG, "enqueue(): nothing to upload, skipping");
            return;
        }

        stopped = false;

        int uploaded = 0;
        int failed = 0;

        UploadSnapshot old = uploadCache.getSnapshot(albumId);
        if (old != null) {
            photos += old.photos;
            videos += old.videos;
            uploaded = old.uploaded;
            failed = old.failed;
        }

        UploadSnapshot snapshot = new UploadSnapshot(
                albumId,
                albumName,
                photos,
                videos,
                uploaded,
                failed
        );

        uploadCache.putSnapshot(snapshot);
        uploadCache.clearStopReason();
        emitIfActive(albumId, snapshot);
        notifyNotification();

        for (MediaSelection s : selections) {
            queue.add(new UploadTask(albumId, s));
        }

        Log.d(TAG, "enqueue(): queueSize=" + queue.size());

        if (currentTask == null) {
            startNext();
        }
    }

    /* ================= Execution ================= */

    private void startNext() {
        if (stopped || queue.isEmpty()) {
            currentTask = null;
            Log.d(TAG, "startNext(): stopped=" + stopped + ", queue empty");
            notifyNotification();
            return;
        }

        currentTask = queue.poll();
        if (currentTask != null) {
            Log.d(TAG, "startNext(): executing albumId=" + currentTask.albumId);
            executeCurrent();
        }
    }

    private void executeCurrent() {
        if (currentTask == null || stopped) return;

        final UploadTask task = currentTask;

        Log.d(TAG, "executeCurrent(): dummy upload started for albumId=" + task.albumId);

        // Simulate 1 second upload
        handler.postDelayed(() -> {

            // Guard: upload cancelled or task changed
            if (stopped || currentTask != task) {
                Log.d(TAG, "executeCurrent(): aborted (cancelled or replaced)");
                return;
            }

            boolean success = random.nextBoolean(); // 50/50

            if (success) {
                Log.d(TAG, "executeCurrent(): dummy SUCCESS");
                onUploadSuccess();
            } else {
                Log.d(TAG, "executeCurrent(): dummy FAILURE");
                onUploadFailure();
            }

        }, 1000);
    }


    void onUploadSuccess() {
        if (currentTask == null) return;

        Log.d(TAG, "onUploadSuccess(): albumId=" + currentTask.albumId);

        UploadSnapshot old = uploadCache.getSnapshot(currentTask.albumId);
        if (old == null) return;

        UploadSnapshot updated = new UploadSnapshot(
                old.albumId,
                old.albumName,
                old.photos,
                old.videos,
                old.uploaded + 1,
                old.failed
        );

        uploadCache.putSnapshot(updated);
        emitIfActive(updated.albumId, updated);
        notifyNotification();

        currentTask = null;
        startNext();
    }

    void onUploadFailure() {
        if (currentTask == null) return;

        Log.w(TAG, "onUploadFailure(): albumId=" + currentTask.albumId +
                ", uri=" + currentTask.selection.uri);

        UploadSnapshot old = uploadCache.getSnapshot(currentTask.albumId);
        if (old == null) return;

        UploadSnapshot updated = new UploadSnapshot(
                old.albumId,
                old.albumName,
                old.photos,
                old.videos,
                old.uploaded,
                old.failed + 1
        );

        uploadCache.putSnapshot(updated);
        emitIfActive(updated.albumId, updated);
        notifyNotification();

        retryStore.addRetry(
                currentTask.albumId,
                currentTask.selection
        );
        retryStore.flush();

        Log.d(TAG, "onUploadFailure(): retry saved & flushed");

        currentTask = null;
        startNext();
    }

    /* ================= Cancel ================= */

    public void cancelAllUploads() {
        Log.w(TAG, "cancelAllByUser(): cancelling uploads");

        stopped = true;
        uploadCache.markStopped(UploadCache.StopReason.USER);

        int retryCount = 0;

        if (currentTask != null) {
            retryStore.addRetry(
                    currentTask.albumId,
                    currentTask.selection
            );
            retryCount++;
        }

        UploadTask t;
        while ((t = queue.poll()) != null) {
            retryStore.addRetry(t.albumId, t.selection);
            retryCount++;
        }

        retryStore.flush();

        Log.d(TAG, "cancelAllByUser(): retriesAdded=" + retryCount);

        currentTask = null;

        // Stable snapshot copy (prevents CME)
        List<UploadSnapshot> finalSnapshots =
                new ArrayList<>(uploadCache.getAllSnapshots().values());

        // 🔁 Single loop: emit + remove
        for (UploadSnapshot snapshot : finalSnapshots) {
            emitIfActive(snapshot.albumId, snapshot);
            uploadCache.removeSnapshot(snapshot.albumId);
        }

        uploadCache.clearStopReason();

        // Notify AFTER final truth is established
        notifyNotification();
    }

}
