package com.github.jaykkumar01.vaultspace.album.upload;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.session.cache.UploadCache;
import com.github.jaykkumar01.vaultspace.core.session.cache.UploadRetryCache;
import com.github.jaykkumar01.vaultspace.models.MediaSelection;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public final class UploadManager {

    private final UploadCache uploadCache;
    private final UploadRetryCache retryCache;

    private final Map<String, UploadObserver> observers = new HashMap<>();
    private final Deque<UploadTask> queue = new ArrayDeque<>();

    private UploadTask currentTask;
    private boolean stopped;

    public UploadManager(
            @NonNull UploadCache uploadCache,
            @NonNull UploadRetryCache retryCache
    ) {
        this.uploadCache = uploadCache;
        this.retryCache = retryCache;
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
            @NonNull Iterable<MediaSelection> selections
    ) {
        int photos = 0, videos = 0;
        for (MediaSelection s : selections) {
            if (s.isVideo) videos++;
            else photos++;
        }
        if (photos + videos == 0) return;

        stopped = false; // reset cancellation

        UploadSnapshot snapshot = new UploadSnapshot(
                albumId,
                photos,
                videos,
                0,
                0
        );

        uploadCache.putSnapshot(snapshot);
        uploadCache.clearStopReason();
        emitIfActive(albumId, snapshot);

        for (MediaSelection s : selections) {
            queue.add(new UploadTask(albumId, s));
        }

        if (currentTask == null) {
            startNext();
        }
    }

    /* ================= Execution ================= */

    private void startNext() {
        if (stopped || queue.isEmpty()) {
            currentTask = null;
            return;
        }

        currentTask = queue.poll();
        if (currentTask != null) {
            executeCurrent();
        }
    }

    private void executeCurrent() {
        // real upload logic will go here
    }

    void onUploadSuccess() {
        if (currentTask == null) return;

        UploadSnapshot old = uploadCache.getSnapshot(currentTask.albumId);
        if (old == null) return;

        UploadSnapshot updated = new UploadSnapshot(
                old.albumId,
                old.photos,
                old.videos,
                old.uploaded + 1,
                old.failed
        );

        uploadCache.putSnapshot(updated);
        emitIfActive(updated.albumId, updated);

        currentTask = null;
        startNext();
    }

    void onUploadFailure() {
        if (currentTask == null) return;

        UploadSnapshot old = uploadCache.getSnapshot(currentTask.albumId);
        if (old == null) return;

        UploadSnapshot updated = new UploadSnapshot(
                old.albumId,
                old.photos,
                old.videos,
                old.uploaded,
                old.failed + 1
        );

        uploadCache.putSnapshot(updated);
        emitIfActive(updated.albumId, updated);

        retryCache.addRetry(
                currentTask.albumId,
                currentTask.selection
        );

        currentTask = null;
        startNext();
    }

    /* ================= Cancel ================= */

    public void cancelAllByUser() {
        stopped = true;
        uploadCache.markStopped(UploadCache.StopReason.USER);

        if (currentTask != null) {
            retryCache.addRetry(
                    currentTask.albumId,
                    currentTask.selection
            );
        }

        while (!queue.isEmpty()) {
            UploadTask t = queue.poll();
            retryCache.addRetry(t.albumId, t.selection);
        }

        currentTask = null;
        emitFinalSnapshots();
    }

    private void emitFinalSnapshots() {
        for (UploadSnapshot s : uploadCache.getAllSnapshots().values()) {
            emitIfActive(s.albumId, s);
        }
    }
}
