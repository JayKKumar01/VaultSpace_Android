package com.github.jaykkumar01.vaultspace.core.upload;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.session.UploadRetryStore;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.core.session.cache.UploadCache;
import com.github.jaykkumar01.vaultspace.core.upload.base.FailureReason;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadObserver;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSnapshot;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadedItem;
import com.github.jaykkumar01.vaultspace.core.upload.helper.UploadDispatcher;
import com.github.jaykkumar01.vaultspace.core.upload.helper.UploadFailureCoordinator;
import com.github.jaykkumar01.vaultspace.core.upload.helper.UploadSnapshotReducer;
import com.github.jaykkumar01.vaultspace.core.upload.helper.UploadTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class UploadManager implements UploadTask.Callback {

    /* ==========================================================
     * Constants
     * ========================================================== */

    private static final String TAG = "VaultSpace:UploadManager";

    /* ==========================================================
     * Fields
     * ========================================================== */

    private final ExecutorService controlExecutor = Executors.newSingleThreadExecutor();
    private final UploadDispatcher dispatcher;


    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Map<String, UploadObserver> observers = new ConcurrentHashMap<>();

    private final UploadCache uploadCache;

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

        snapshotReducer = new UploadSnapshotReducer(appContext, uploadCache, retryStore);

        failureCoordinator = new UploadFailureCoordinator(appContext, uploadCache, retryStore);
        dispatcher = new UploadDispatcher(context);
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
            uploadCache.clearStopReason();

            UploadSnapshot snapshot = snapshotReducer.mergeSnapshot(groupId, groupName, selections);

            emitSnapshot(groupId, snapshot);

            uploadCache.putSnapshot(snapshot);
            notifyStateChanged();

            failureCoordinator.recordRetriesIfMissing(selections);

            dispatcher.enqueue(selections,this);

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

    @Override
    public void onSuccess(String gid, UploadSelection s, UploadedItem item){
        controlExecutor.execute(()->{
            orchestrator.dispatchUploadSuccess(gid,item);
            emitSuccess(gid,item);
            UploadSnapshot u = snapshotReducer.onSuccess(gid,s);
            snapshotReducer.finalizeSnapshot(u);
            emitSnapshot(gid,u);
            notifyStateChanged();
        });
    }

    @Override
    public void onFailure(String gid, UploadSelection s, FailureReason r){
        controlExecutor.execute(()->{
            orchestrator.dispatchUploadFailure(gid,s,r);
            emitFailure(gid,s);
            failureCoordinator.updateReason(s, r);
            UploadSnapshot u = snapshotReducer.onFailure(gid,r);
            snapshotReducer.finalizeSnapshot(u);
            emitSnapshot(gid,u);
            notifyStateChanged();
        });
    }


    @Override
    public void onProgress(String uId, String gid, UploadSelection selection, long u, long t){
        UploadObserver o = observers.get(gid);
        if(o!=null) mainHandler.post(()->o.onProgress(selection, u,t));
    }





    /* ==========================================================
     * Cancel API
     * ========================================================== */

    public void cancelUploads(@NonNull String groupId) {
        controlExecutor.execute(() -> {
            dispatcher.cancelGroup(groupId);
            failureCoordinator.clearGroup(groupId);
            uploadCache.removeSnapshot(groupId);
            emitCancelled(groupId);
            notifyStateChanged();
        });
    }

    public void cancelAllUploads() {
        controlExecutor.execute(() -> {
            dispatcher.cancelAll();
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
        dispatcher.shutdown();
        controlExecutor.shutdownNow();
    }

}
