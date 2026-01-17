package com.github.jaykkumar01.vaultspace.core.upload;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.session.UploadRetryStore;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.core.session.cache.UploadCache;
import com.github.jaykkumar01.vaultspace.models.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.utils.UriUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class UploadManager {

    private final Context appContext;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Map<String, UploadObserver> observers = new ConcurrentHashMap<>();
    private final UploadCache uploadCache;
    private final UploadRetryStore retryStore;

    private final Deque<UploadTask> queue = new ArrayDeque<>();
    private UploadTask current;
    private UploadOrchestrator orchestrator;

    public UploadManager(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        UserSession session = new UserSession(appContext);
        this.uploadCache = session.getVaultCache().uploadCache;
        this.retryStore = session.getUploadRetryStore();
    }

    public void attachOrchestrator(@NonNull UploadOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    private void notifyStateChanged() {
        if (orchestrator != null) mainHandler.post(orchestrator::onUploadStateChanged);
    }

    /* ================= Observer ================= */

    public void registerObserver(@NonNull String groupId, @NonNull String groupName, @NonNull UploadObserver observer) {
        executor.execute(() -> {
            observers.put(groupId, observer);

            UploadSnapshot snapshot = uploadCache.getSnapshot(groupId);
            if (snapshot != null) {
                emitSnapshot(groupId, snapshot);
                return;
            }

            restoreFromRetry(groupId, groupName);
        });
    }

    public void unregisterObserver(@NonNull String groupId) {
        executor.execute(() -> observers.remove(groupId));
    }

    /* ================= Enqueue ================= */

    public void enqueue(@NonNull String groupId, @NonNull String groupName, @NonNull List<? extends UploadSelection> selections) {
        executor.execute(() -> {
            UploadSnapshot snapshot = mergeSnapshot(groupId, groupName, selections);
            uploadCache.putSnapshot(snapshot);
            emitSnapshot(groupId, snapshot);

            for (UploadSelection s : selections) queue.add(new UploadTask(groupId, s));
            if (current == null) startNext();

            notifyStateChanged();
        });
    }

    /* ================= Execution ================= */

    private void startNext() {
        if (queue.isEmpty()) {
            current = null;
            notifyStateChanged();
            return;
        }
        current = queue.poll();
        performUpload(current);
    }

    private void performUpload(UploadTask task){
        executor.execute(() -> {
            try{ Thread.sleep(2000); }
            catch(InterruptedException ignored){}
            if(Math.random()>0.5) handleSuccess(task);
            else handleFailure(task);
        });
    }

    private void handleSuccess(UploadTask task){
        UploadSnapshot old=uploadCache.getSnapshot(task.groupId);
        if(old==null){ startNext(); return; }

        UploadSnapshot updated=new UploadSnapshot(
                old.groupId,old.groupName,
                old.photos,old.videos,old.others,
                old.uploaded+1,old.failed
        );
        updated.nonRetryableFailed=old.nonRetryableFailed;

        finalizeStep(task.groupId,updated);
    }

    private void handleFailure(UploadTask task){
        UploadSnapshot old=uploadCache.getSnapshot(task.groupId);
        if(old==null){ startNext(); return; }

        boolean retryable=UriUtils.isUriAccessible(appContext,task.selection.uri);

        UploadSnapshot updated=new UploadSnapshot(
                old.groupId,old.groupName,
                old.photos,old.videos,old.others,
                old.uploaded,old.failed+1
        );
        updated.nonRetryableFailed=old.nonRetryableFailed;

        if(retryable) retryStore.addRetry(task.groupId,task.selection);
        else updated.nonRetryableFailed++;

        retryStore.flush();
        finalizeStep(task.groupId,updated);
    }

    private void finalizeStep(String groupId,UploadSnapshot snapshot){
        uploadCache.putSnapshot(snapshot);
        emitSnapshot(groupId,snapshot);
        notifyStateChanged();

        current=null;

        startNext();
    }

    /* ================= Restore ================= */

    private void restoreFromRetry(String groupId, String groupName) {
        List<UploadSelection> list = retryStore.getAllRetries().get(groupId);
        if (list == null || list.isEmpty()) return;

        List<UploadSelection> valid = new ArrayList<>();
        int nonRetryable = 0, photos = 0, videos = 0, others = 0;

        for (UploadSelection s : list) {
            if (UriUtils.isUriAccessible(appContext, s.uri)) valid.add(s);
            else nonRetryable++;

            switch (s.getType()) {
                case PHOTO -> photos++;
                case VIDEO -> videos++;
                case FILE -> others++;
            }
        }

//        retryStore.replaceGroupRetries(groupId, valid);
//        retryStore.flush();

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
        executor.execute(() -> {
            queue.removeIf(t -> t.groupId.equals(groupId));
            if (current != null && current.groupId.equals(groupId)) current = null;

            uploadCache.removeSnapshot(groupId);
            retryStore.clearGroup(groupId);
            retryStore.flush();

            emitCancelled(groupId);
            startNext();
            notifyStateChanged();
        });
    }

    public void cancelAllUploads() {
        executor.execute(() -> {
            queue.clear();
            current = null;

            for (String groupId : uploadCache.getAllSnapshots().keySet()) {
                emitCancelled(groupId);
                uploadCache.removeSnapshot(groupId);
                retryStore.clearGroup(groupId);
            }

            retryStore.flush();
            uploadCache.markStopped(UploadCache.StopReason.USER);
            notifyStateChanged();
        });
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    /* ================= Utils ================= */

    private UploadSnapshot mergeSnapshot(String groupId, String groupName, List<? extends UploadSelection> selections) {
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

    public void removeSnapshotFromStore(String groupId){
        uploadCache.removeSnapshot(groupId);
        retryStore.clearGroup(groupId);
        retryStore.flush();
    }




}
