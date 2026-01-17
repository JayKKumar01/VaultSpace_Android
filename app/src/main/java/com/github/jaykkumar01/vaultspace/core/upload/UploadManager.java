package com.github.jaykkumar01.vaultspace.core.upload;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.session.UploadFailureStore;
import com.github.jaykkumar01.vaultspace.core.session.UploadRetryStore;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.core.session.cache.UploadCache;
import com.github.jaykkumar01.vaultspace.core.session.db.UploadFailureEntity;
import com.github.jaykkumar01.vaultspace.core.session.db.UploadFailureReason;
import com.github.jaykkumar01.vaultspace.models.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.utils.UploadMetadataResolver;
import com.github.jaykkumar01.vaultspace.utils.UploadThumbnailGenerator;
import com.github.jaykkumar01.vaultspace.utils.UriUtils;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class UploadManager {
    private static final String TAG = "VaultSpace:UploadManager";

    private final Context appContext;

    private final ExecutorService controlExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService uploadExecutor = Executors.newSingleThreadExecutor();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Map<String, UploadObserver> observers = new ConcurrentHashMap<>();
    private final UploadCache uploadCache;
    private final UploadRetryStore retryStore;
    private final UploadFailureStore failureStore;
    private final File thumbDir;

    private final Deque<UploadTask> queue = new ArrayDeque<>();
    private UploadTask current;
    private UploadOrchestrator orchestrator;
    private Future<?> runningUpload;


    public UploadManager(@NonNull Context context) {
        appContext = context.getApplicationContext();
        UserSession session = new UserSession(appContext);
        uploadCache = session.getVaultCache().uploadCache;
        retryStore = session.getUploadRetryStore();
        failureStore = session.getUploadFailureStore();
        thumbDir = new File(appContext.getFilesDir(), "upload_failures/thumbs");
    }

    public void attachOrchestrator(@NonNull UploadOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    private void notifyStateChanged() {
        if (orchestrator != null)
            mainHandler.post(orchestrator::onUploadStateChanged);
    }

    /* ================= Observer ================= */

    public void registerObserver(@NonNull String groupId, @NonNull String groupName, @NonNull UploadObserver observer) {
        controlExecutor.execute(() -> {
            observers.put(groupId, observer);
            UploadSnapshot snapshot = uploadCache.getSnapshot(groupId);
            if (snapshot != null) emitSnapshot(groupId, snapshot);
            else restoreFromRetry(groupId, groupName);
        });
    }

    public void unregisterObserver(@NonNull String groupId) {
        controlExecutor.execute(() -> observers.remove(groupId));
    }

    /* ================= Enqueue ================= */

    public void enqueue(@NonNull String groupId, @NonNull String groupName, @NonNull List<UploadSelection> selections) {
        controlExecutor.execute(() -> {
            //noinspection ResultOfMethodCallIgnored
            thumbDir.mkdirs();

            List<UploadFailureEntity> failureRows = new ArrayList<>(selections.size());

            for (UploadSelection s : selections) {
                String name = UploadMetadataResolver.resolveDisplayName(appContext, s.uri);
                String thumb = UploadThumbnailGenerator.generate(
                        appContext, s.uri, s.getType(), thumbDir
                );
                failureRows.add(new UploadFailureEntity(
                        0L,
                        groupId,
                        s.uri.toString(),
                        name,
                        s.getType().name(),
                        thumb,
                        UploadFailureReason.UNKNOWN.name()
                ));
            }

            failureStore.addFailures(failureRows);

            retryStore.addRetryBatch(groupId, selections);

            UploadSnapshot snapshot = mergeSnapshot(groupId, groupName, selections);
            uploadCache.putSnapshot(snapshot);
            emitSnapshot(groupId, snapshot);

            for (UploadSelection s : selections)
                queue.add(new UploadTask(groupId, s));

            processQueue();
            notifyStateChanged();
        });
    }

    /* ================= Execution ================= */

    private void processQueue() {
        if (current != null) return;
        if (queue.isEmpty()) {
            notifyStateChanged();
            return;
        }

        current = queue.poll();
        UploadTask task = current;

        runningUpload = uploadExecutor.submit(() -> performUpload(task));

    }


    private void performUpload(UploadTask task) {
        try {
            Thread.sleep(1000); // simulate upload
            if (Math.random() > 0.5)
                controlExecutor.execute(() -> handleSuccess(task));
            else
                controlExecutor.execute(() -> handleFailure(task));
        } catch (InterruptedException e) {
            // upload was cancelled
            controlExecutor.execute(() -> handleCancelled(task));
        }
    }

    private void handleCancelled(UploadTask task) {
        // Ignore stale cancellation
        if (current != task) {
            Log.w(TAG, "Ignoring stale cancellation for " + task);
            return;
        }

        runningUpload = null;
        current = null;
        processQueue();
    }


    private void handleSuccess(UploadTask task) {
        if (current != task) return;

        UploadSnapshot old = uploadCache.getSnapshot(task.groupId);
        if (old == null) {
            current = null;
            processQueue();
            return;
        }

        retryStore.removeRetry(task.groupId, task.selection);
        failureStore.removeFailure(task.groupId,task.selection.uri.toString(),task.selection.getType().name());

        UploadSnapshot updated = new UploadSnapshot(
                old.groupId, old.groupName,
                old.photos, old.videos, old.others,
                old.uploaded + 1, old.failed
        );
        updated.nonRetryableFailed = old.nonRetryableFailed;
        finalizeStep(task.groupId, updated);
    }

    private void handleFailure(UploadTask task) {
        if (current != task) return;

        UploadSnapshot old = uploadCache.getSnapshot(task.groupId);
        if (old == null) {
            current = null;
            processQueue();
            return;
        }

        boolean retryable = UriUtils.isUriAccessible(appContext, task.selection.uri);

        UploadSnapshot updated = new UploadSnapshot(
                old.groupId, old.groupName,
                old.photos, old.videos, old.others,
                old.uploaded, old.failed + 1
        );
        updated.nonRetryableFailed = old.nonRetryableFailed;

        if (!retryable) {
            updated.nonRetryableFailed++;
        }
        finalizeStep(task.groupId, updated);
    }


    private void finalizeStep(String groupId, UploadSnapshot snapshot) {
        uploadCache.putSnapshot(snapshot);
        emitSnapshot(groupId, snapshot);
        notifyStateChanged();

        runningUpload = null;   // ✅ ADD THIS LINE
        current = null;
        processQueue();
    }


    /* ================= Restore ================= */

    private void restoreFromRetry(String groupId, String groupName) {
        List<UploadSelection> list = retryStore.getAllRetries().get(groupId);
        if (list == null || list.isEmpty()) return;

        int nonRetryable = 0, photos = 0, videos = 0, others = 0;

        for (UploadSelection s : list) {
            if (!UriUtils.isUriAccessible(appContext, s.uri)) nonRetryable++;

            switch (s.getType()) {
                case PHOTO -> photos++;
                case VIDEO -> videos++;
                case FILE -> others++;
            }
        }

        UploadSnapshot snapshot = new UploadSnapshot(
                groupId, groupName,
                photos, videos, others,
                0, photos + videos + others
        );
        snapshot.nonRetryableFailed = nonRetryable;
        uploadCache.putSnapshot(snapshot);
        emitSnapshot(groupId, snapshot);
    }

    /* ================= Cancel ================= */

    public void cancelUploads(@NonNull String groupId) {

        controlExecutor.execute(() -> {
            queue.removeIf(t -> t.groupId.equals(groupId));
            if (current != null && current.groupId.equals(groupId)) {
                if (runningUpload != null) {
                    runningUpload.cancel(true); // 🔴 THIS interrupts sleep / IO
                    runningUpload = null;
                }
                current = null;
            }

            uploadCache.removeSnapshot(groupId);
            retryStore.clearGroup(groupId);

            emitCancelled(groupId);
            processQueue();
            notifyStateChanged();
        });
    }

    public void cancelAllUploads() {
        controlExecutor.execute(() -> {
            // 1️⃣ Stop execution
            queue.clear();
            if (runningUpload != null) {
                runningUpload.cancel(true);
                runningUpload = null;
            }
            current = null;

            // 2️⃣ Snapshot groupIds defensively
            List<String> groupIds =
                    new ArrayList<>(uploadCache.getAllSnapshots().keySet());

            // 3️⃣ Cancel + clear per group
            for (String groupId : groupIds) {
                emitCancelled(groupId);
                uploadCache.removeSnapshot(groupId);
                retryStore.clearGroup(groupId);
            }

            // 5️⃣ Mark system state
            uploadCache.markStopped(UploadCache.StopReason.USER);
            notifyStateChanged();
        });
    }

    public void shutdown() {
        controlExecutor.shutdownNow();
        uploadExecutor.shutdownNow();
    }

    /* ================= Utils ================= */

    private UploadSnapshot mergeSnapshot(String groupId, String groupName, List<UploadSelection> selections) {
        int photos = 0, videos = 0, others = 0;
        for (UploadSelection s : selections) {
            switch (s.getType()) {
                case PHOTO -> photos++;
                case VIDEO -> videos++;
                case FILE -> others++;
            }
        }

        UploadSnapshot old = uploadCache.getSnapshot(groupId);
        int uploaded = 0, failed = 0;
        if (old != null) {
            photos += old.photos;
            videos += old.videos;
            others += old.others;
            uploaded = old.uploaded;
            failed = old.failed;
        }

        return new UploadSnapshot(groupId, groupName, photos, videos, others, uploaded, failed);
    }

    private void emitSnapshot(String groupId, UploadSnapshot snapshot) {
        UploadObserver o = observers.get(groupId);
        if (o != null) mainHandler.post(() -> o.onSnapshot(snapshot));
    }

    private void emitCancelled(String groupId) {
        UploadObserver o = observers.get(groupId);
        if (o != null) mainHandler.post(o::onCancelled);
    }

    public void removeSnapshotFromCache(String groupId) {
        uploadCache.removeSnapshot(groupId);
    }

    public void removeSnapshotFromStore(String groupId) {
        uploadCache.removeSnapshot(groupId);
        retryStore.clearGroup(groupId);
    }

    public void removeUploadFailureMetadata(String groupId) {
        uploadCache.removeSnapshot(groupId);
        retryStore.clearGroup(groupId);
        failureStore.clearGroup(groupId);
        // also remove the thumnails from disk
    }

    public void getNonRetryableFailures(String groupId, Object o) {
    }
}
