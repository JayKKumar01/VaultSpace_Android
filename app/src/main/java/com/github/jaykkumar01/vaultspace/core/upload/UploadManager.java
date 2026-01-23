package com.github.jaykkumar01.vaultspace.core.upload;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.session.UploadRetryStore;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.core.session.cache.UploadCache;
import com.github.jaykkumar01.vaultspace.core.upload.base.FailureReason;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadedItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class UploadManager implements UploadQueueEngine.Callback {

    /* ==========================================================
     * Constants
     * ========================================================== */

    private static final String TAG = "VaultSpace:UploadManager";

    /* ==========================================================
     * Fields
     * ========================================================== */

    private final ExecutorService controlExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService uploadExecutor = Executors.newSingleThreadExecutor();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Map<String, UploadObserver> observers = new ConcurrentHashMap<>();

    private final UploadCache uploadCache;
    private final File thumbDir;

    private final UploadQueueEngine queueEngine;
    private final UploadSnapshotReducer snapshotReducer;
    private final UploadRetryStore retryStore;

    private final UploadFailureCoordinator failureCoordinator;

    private UploadOrchestrator orchestrator;

    /* ==========================================================
     * Constructor
     * ========================================================== */

    public UploadManager(@NonNull Context context) {
        Context appContext = context.getApplicationContext();

        UserSession session = new UserSession(appContext);
        uploadCache = session.getVaultCache().uploadCache;

        this.retryStore = session.getUploadRetryStore();

        thumbDir = new File(appContext.getFilesDir(), "upload_failures/thumbs");

        queueEngine = new UploadQueueEngine(appContext, controlExecutor, uploadExecutor);
        queueEngine.setCallback(this);

        snapshotReducer = new UploadSnapshotReducer(appContext, uploadCache, retryStore);

        failureCoordinator = new UploadFailureCoordinator(appContext, uploadCache, retryStore);
    }

    /* ==========================================================
     * Wiring
     * ========================================================== */

    public void attachOrchestrator(@NonNull UploadOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /* ==========================================================
     * Observer API
     * ========================================================== */

    public void registerObserver(
            @NonNull String groupId,
            @NonNull String groupName,
            @NonNull UploadObserver observer
    ) {
        controlExecutor.execute(() -> {
            observers.put(groupId, observer);

            UploadSnapshot snapshot = uploadCache.getSnapshot(groupId);
            if (snapshot != null) {
                emitSnapshot(groupId, snapshot);
            } else {
                UploadSnapshot restored =
                        failureCoordinator.restoreFromRetry(groupId, groupName);
                if (restored != null)
                    emitSnapshot(groupId, restored);
            }
        });
    }

    public void unregisterObserver(@NonNull String groupId) {
        controlExecutor.execute(() -> observers.remove(groupId));
    }

    /* ==========================================================
     * Enqueue / Retry API
     * ========================================================== */

    public void enqueue(
            @NonNull String groupId,
            @NonNull String groupName,
            @NonNull List<UploadSelection> selections
    ) {
        controlExecutor.execute(() -> {

            UploadSnapshot snapshot = snapshotReducer.mergeSnapshot(groupId, groupName, selections);

            emitSnapshot(groupId, snapshot);

            uploadCache.putSnapshot(snapshot);
            notifyStateChanged();

            //noinspection ResultOfMethodCallIgnored
            thumbDir.mkdirs();

            failureCoordinator.recordRetriesIfMissing(groupId, selections);

            queueEngine.enqueue(groupId, selections);
            queueEngine.processQueue();
        });
    }

    public void retry(@NonNull String groupId, @NonNull String groupName) {
        controlExecutor.execute(() -> {
            List<UploadSelection> retryable =
                    failureCoordinator.retry(groupId, groupName);
            if (retryable != null && !retryable.isEmpty())
                enqueue(groupId, groupName, retryable);
        });
    }

    /* ==========================================================
     * Queue callbacks
     * ========================================================== */

    @Override
    public void onSuccess(UploadTask task, UploadedItem uploadedItem) {

        if (orchestrator != null)
            orchestrator.dispatchUploadSuccess(task.groupId, uploadedItem);

        emitSuccess(task.groupId, uploadedItem);

        UploadSnapshot snapshot = snapshotReducer.onSuccess(task);
        if (snapshot != null) {
            snapshotReducer.finalizeSnapshot(snapshot);
            emitSnapshot(task.groupId, snapshot);
        }

        notifyStateChanged();
    }

    @Override
    public void onFailure(UploadTask task, FailureReason reason) {

        if (orchestrator != null) {
            orchestrator.dispatchUploadFailure(task.groupId, task.selection, reason);
        }

        emitFailure(task.groupId, task.selection);

        UploadSnapshot snapshot = snapshotReducer.onFailure(task.groupId, reason);
        failureCoordinator.updateReason(task, reason);

        if (snapshot != null) {
            snapshotReducer.finalizeSnapshot(snapshot);
            emitSnapshot(task.groupId, snapshot);
        }

        notifyStateChanged();
    }

    @Override
    public void onCancelled(UploadTask task) {
        notifyStateChanged();
    }

    @Override
    public void onIdle() {
        notifyStateChanged();
    }

    @Override
    public void onProgress(String groupId, String name, long uploadedBytes, long totalBytes) {
        emitProgress(groupId, name, uploadedBytes, totalBytes);
    }

    private void emitProgress(String groupId, String name, long uploadedBytes, long totalBytes) {
        UploadObserver o = observers.get(groupId);
        if (o != null) mainHandler.post(() -> o.onProgress(name, uploadedBytes, totalBytes));
    }



    /* ==========================================================
     * Cancel API
     * ========================================================== */

    public void cancelUploads(@NonNull String groupId) {
        controlExecutor.execute(() -> {
            queueEngine.cancelGroup(groupId);
            failureCoordinator.clearGroup(groupId);
            uploadCache.removeSnapshot(groupId);
            emitCancelled(groupId);
            queueEngine.processQueue();
            notifyStateChanged();
        });
    }

    public void cancelAllUploads() {
        controlExecutor.execute(() -> {
            queueEngine.cancelAll();
            for (String groupId : new ArrayList<>(uploadCache.getAllSnapshots().keySet())) {
                emitCancelled(groupId);
                failureCoordinator.clearGroup(groupId);
                uploadCache.removeSnapshot(groupId);
            }
            uploadCache.markStopped(UploadCache.StopReason.USER);
            notifyStateChanged();
        });
    }

    /* ==========================================================
     * Internal emit helpers
     * ========================================================== */

    private void emitSnapshot(String groupId, UploadSnapshot snapshot) {
        UploadObserver o = observers.get(groupId);
        if (o != null)
            mainHandler.post(() -> o.onSnapshot(snapshot));
    }

    private void emitCancelled(String groupId) {
        UploadObserver o = observers.get(groupId);
        if (o != null)
            mainHandler.post(o::onCancelled);
    }

    private void emitSuccess(String groupId, UploadedItem item) {
        UploadObserver o = observers.get(groupId);
        if (o != null)
            mainHandler.post(() -> o.onSuccess(item));
    }

    private void emitFailure(String groupId, UploadSelection selection) {
        UploadObserver o = observers.get(groupId);
        if (o != null)
            mainHandler.post(() -> o.onFailure(selection));
    }

    private void notifyStateChanged() {
        if (orchestrator != null)
            mainHandler.post(orchestrator::onUploadStateChanged);
    }

    /* ==========================================================
     * Cleanup
     * ========================================================== */

    public void clearGroup(String groupId) {
        controlExecutor.execute(() -> {
            failureCoordinator.clearGroup(groupId);
            uploadCache.removeSnapshot(groupId);
        });
    }

    public void getFailuresForGroup(
            @NonNull String groupId,
            @NonNull Consumer<List<UploadSelection>> cb
    ) {
        controlExecutor.execute(() -> {
            List<UploadSelection> list = retryStore.getRetriesForGroup(groupId);
            mainHandler.post(() -> cb.accept(list));
        });
    }

    public void shutdown() {
        controlExecutor.shutdownNow();
        uploadExecutor.shutdownNow();
        queueEngine.cancelAll();
    }
}
