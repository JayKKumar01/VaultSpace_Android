//package com.github.jaykkumar01.vaultspace.core.upload;
//
//import android.content.Context;
//import android.os.Handler;
//import android.os.Looper;
//import android.util.Log;
//
//import androidx.annotation.NonNull;
//
//import com.github.jaykkumar01.vaultspace.core.session.UploadRetryStore;
//import com.github.jaykkumar01.vaultspace.core.session.cache.UploadCache;
//import com.github.jaykkumar01.vaultspace.models.MediaSelection;
//import com.github.jaykkumar01.vaultspace.models.base.UploadSelection;
//import com.github.jaykkumar01.vaultspace.utils.UriUtils;
//
//import java.util.ArrayDeque;
//import java.util.ArrayList;
//import java.util.Deque;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Random;
//
//public final class UploadManagerOld {
//
//    private static final String TAG = "VaultSpace:UploadMgr";
//
//    private final Context appContext;
//    private final UploadCache uploadCache;
//    private final UploadRetryStore retryStore;
//    private final Map<String, UploadObserver> observers = new HashMap<>();
//    private final Deque<UploadTask> queue = new ArrayDeque<>();
//    private final Map<String, List<MediaSelection>> allRetries;
//
//    private UploadTask currentTask;
//    private boolean stopped;
//
//    private UploadOrchestrator orchestrator;
//
//    private final Handler handler = new Handler(Looper.getMainLooper());
//    private final Random random = new Random();
//
//    public UploadManagerOld(
//            @NonNull Context context,
//            @NonNull UploadCache uploadCache,
//            @NonNull UploadRetryStore retryStore
//    ) {
//        this.appContext = context.getApplicationContext();
//        this.uploadCache = uploadCache;
//        this.retryStore = retryStore;
//        this.allRetries = retryStore.getAllRetries();
//    }
//
//    /* ================= Wiring ================= */
//
//    public void attachOrchestrator(
//            @NonNull UploadOrchestrator orchestrator
//    ) {
//        this.orchestrator = orchestrator;
//    }
//
//    private void notifyNotification() {
//        if (orchestrator != null) {
//            orchestrator.onUploadStateChanged();
//        }
//    }
//
//    /* ================= Retry Healing ================= */
//
//    @NonNull
//    private List<MediaSelection> healRetryStoreIfNeeded(
//            @NonNull String groupId,
//            @NonNull List<MediaSelection> retries
//    ) {
//        int total = retries.size();
//        List<MediaSelection> valid = new ArrayList<>(total);
//        int invalidCount = 0;
//
//        for (MediaSelection s : retries) {
//            if (UriUtils.isUriAccessible(appContext, s.uri)) {
//                valid.add(s);
//            } else {
//                invalidCount++;
//            }
//        }
//
//        if (invalidCount > 0) {
//            Log.w(
//                    TAG,
//                    "healRetryStoreIfNeeded(): groupId=" + groupId +
//                            ", healed retries (valid=" + valid.size() +
//                            ", dropped=" + invalidCount + ")"
//            );
//
//            retryStore.replaceAlbumRetries(groupId, valid);
//            retryStore.flush();
//        }
//
//        return valid;
//    }
//
//
//    /* ================= Observer ================= */
//
//    public void registerObserver(
//            @NonNull String groupId,
//            @NonNull String albumName,
//            @NonNull UploadObserver observer
//    ) {
//        observers.put(groupId, observer);
//
//        UploadSnapshot snapshot = uploadCache.getSnapshot(groupId);
//        if (snapshot != null) {
//            observer.onSnapshot(snapshot);
//            return;
//        }
//
//        List<MediaSelection> retryList = allRetries.get(groupId);
//        if (retryList == null || retryList.isEmpty()) return;
//
//        List<MediaSelection> validRetries =
//                healRetryStoreIfNeeded(groupId, retryList);
//
//        if (validRetries.isEmpty()) return;
//
//        int photos = 0;
//        int videos = 0;
//
//        for (MediaSelection s : validRetries) {
//            if (s.isVideo) videos++;
//            else photos++;
//        }
//
//        UploadSnapshot retrySnapshot = new UploadSnapshot(
//                groupId,
//                albumName,
//                photos,
//                videos,
//                0,
//                validRetries.size()
//        );
//
//        uploadCache.putSnapshot(retrySnapshot);
//        observer.onSnapshot(retrySnapshot);
//    }
//
//    public void unregisterObserver(@NonNull String groupId) {
//        observers.remove(groupId);
//    }
//
//    private void emitIfActive(
//            @NonNull String groupId,
//            @NonNull UploadSnapshot snapshot
//    ) {
//        UploadObserver observer = observers.get(groupId);
//        if (observer != null) {
//            observer.onSnapshot(snapshot);
//        }
//    }
//
//    private void emitCancelled(@NonNull String groupId) {
//        UploadObserver observer = observers.get(groupId);
//        if (observer != null) {
//            observer.onCancelled();
//        }
//    }
//
//    /* ================= Enqueue ================= */
//
//    public void enqueue(
//            @NonNull String groupId,
//            @NonNull String albumName,
//            @NonNull List<? extends UploadSelection> selections
//    ) {
//        Log.d(TAG, "enqueue(): groupId=" + groupId);
//
//        int photos = 0;
//        int videos = 0;
//
//        for (MediaSelection s : selections) {
//            if (s.isVideo) videos++;
//            else photos++;
//        }
//
//        stopped = false;
//
//        int uploaded = 0;
//        int failed = 0;
//
//        UploadSnapshot old = uploadCache.getSnapshot(groupId);
//        if (old != null) {
//            photos += old.photos;
//            videos += old.videos;
//            uploaded = old.uploaded;
//            failed = old.failed;
//        }
//
//        UploadSnapshot snapshot = new UploadSnapshot(
//                groupId,
//                albumName,
//                photos,
//                videos,
//                uploaded,
//                failed
//        );
//
//        uploadCache.putSnapshot(snapshot);
//        uploadCache.clearStopReason();
//        emitIfActive(groupId, snapshot);
//        notifyNotification();
//
//        for (MediaSelection s : validSelections) {
//            queue.add(new UploadTask(groupId, s));
//        }
//
//        if (currentTask == null) {
//            startNext();
//        }
//    }
//
//    /* ================= Execution ================= */
//
//    private void startNext() {
//        if (stopped || queue.isEmpty()) {
//            currentTask = null;
//            notifyNotification();
//            return;
//        }
//
//        currentTask = queue.poll();
//        executeCurrent();
//    }
//
//    private void executeCurrent() {
//        if (currentTask == null || stopped) return;
//
//        final UploadTask task = currentTask;
//
//        handler.postDelayed(() -> {
//            if (stopped || currentTask != task) return;
//
//            if (random.nextBoolean()) {
//                onUploadSuccess();
//            } else {
//                onUploadFailure();
//            }
//        }, 1000);
//    }
//
//    void onUploadSuccess() {
//        if (currentTask == null) return;
//
//        UploadSnapshot old = uploadCache.getSnapshot(currentTask.groupId);
//        if (old == null) return;
//
//        UploadSnapshot updated = new UploadSnapshot(
//                old.groupId,
//                old.groupName,
//                old.photos,
//                old.videos,
//                old.uploaded + 1,
//                old.failed
//        );
//
//        uploadCache.putSnapshot(updated);
//        emitIfActive(updated.groupId, updated);
//        notifyNotification();
//
//        currentTask = null;
//        startNext();
//    }
//
//    void onUploadFailure() {
//        if (currentTask == null) return;
//
//        String groupId = currentTask.groupId;
//        MediaSelection selection = currentTask.selection;
//
//        UploadSnapshot old = uploadCache.getSnapshot(groupId);
//        if (old == null) return;
//
//        boolean retryable = UriUtils.isUriAccessible(appContext, selection.uri);
//
//        // 1️⃣ Build updated snapshot AFTER classification
//        UploadSnapshot updated = new UploadSnapshot(
//                old.groupId,
//                old.groupName,
//                old.photos,
//                old.videos,
//                old.uploaded,
//                old.failed + 1
//        );
//
//        // carry forward non-retryable count
//        updated.nonRetryableFailed = old.nonRetryableFailed;
//
//        if (retryable) {
//            Log.w(TAG, "onUploadFailure(): retryable uri=" + selection.uri);
//
//            retryStore.addRetry(groupId, selection);
//            retryStore.flush();
//
//        } else {
//            Log.w(
//                    TAG,
//                    "onUploadFailure(): non-retryable (permission lost) uri=" + selection.uri
//            );
//
//            updated.nonRetryableFailed++;
//        }
//
//        // 2️⃣ Now snapshot is FINAL → emit once
//        uploadCache.putSnapshot(updated);
//        emitIfActive(groupId, updated);
//        notifyNotification();
//
//        // 3️⃣ Continue pipeline
//        currentTask = null;
//        startNext();
//    }
//
//
//
//
//    /* ================= Cancel ================= */
//
//    public void cancelAllUploads() {
//        stopped = true;
//        queue.clear();
//        currentTask = null;
//
//        List<UploadSnapshot> snapshots =
//                new ArrayList<>(uploadCache.getAllSnapshots().values());
//
//        for (UploadSnapshot snapshot : snapshots) {
//            emitCancelled(snapshot.groupId);
//            uploadCache.removeSnapshot(snapshot.groupId);
//        }
//
//        uploadCache.markStopped(UploadCache.StopReason.USER);
//        notifyNotification();
//    }
//
//    public void cancelUploads(@NonNull String groupId) {
//        queue.removeIf(task -> task.groupId.equals(groupId));
//
//        boolean abortCurrent =
//                currentTask != null &&
//                        currentTask.groupId.equals(groupId);
//
//        if (abortCurrent) {
//            currentTask = null;
//        }
//
//        emitCancelled(groupId);
//        uploadCache.removeSnapshot(groupId);
//
//        retryStore.clearAlbum(groupId);
//        retryStore.flush();
//
//        if (abortCurrent) {
//            startNext();
//        }
//
//        notifyNotification();
//    }
//}
