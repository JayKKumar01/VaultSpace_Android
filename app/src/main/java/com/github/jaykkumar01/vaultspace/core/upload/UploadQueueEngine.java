package com.github.jaykkumar01.vaultspace.core.upload;

import android.content.Context;

import com.github.jaykkumar01.vaultspace.core.session.UploadFailureStore;
import com.github.jaykkumar01.vaultspace.core.upload.drive.UploadDriveHelper;
import com.github.jaykkumar01.vaultspace.models.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.models.base.UploadedItem;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public final class UploadQueueEngine {



    public interface Callback {
        void onSuccess(UploadTask task, UploadedItem uploadedItem);

        void onFailure(UploadTask task, UploadDriveHelper.FailureReason reason);

        void onCancelled(UploadTask task);

        void onIdle();

        void onProgress(String groupId,String itemName, long uploadedBytes, long totalBytes);
    }

    private final ExecutorService controlExecutor;
    private final ExecutorService uploadExecutor;
    private final UploadDriveHelper uploadDriveHelper;
    private final UploadFailureStore failureStore;
    private final File thumbDir;

    private final Deque<UploadTask> queue = new ArrayDeque<>();

    private UploadTask current;
    private Future<?> runningUpload;
    private Callback callback;

    UploadQueueEngine(Context appContext, ExecutorService controlExecutor, ExecutorService uploadExecutor, UploadFailureStore failureStore, File thumbDir) {
        this.controlExecutor = controlExecutor;
        this.uploadExecutor = uploadExecutor;
        this.uploadDriveHelper = new UploadDriveHelper(appContext);;
        this.failureStore = failureStore;
        this.thumbDir = thumbDir;
    }

    void setCallback(Callback callback) {
        this.callback = callback;
    }

    void enqueue(String groupId, List<UploadSelection> selections) {
        for (UploadSelection s : selections)
            queue.add(new UploadTask(groupId, s));
    }

    public void processQueue() {
        if (current != null) return;
        if (queue.isEmpty()) {
            if (callback != null) callback.onIdle();
            return;
        }
        current = queue.poll();
        UploadTask task = current;
        runningUpload = uploadExecutor.submit(() -> performUpload(task));
    }


    private void performUpload(UploadTask task) {
        try {
            UploadedItem item = uploadDriveHelper.upload(task.groupId, task.selection,failureStore,thumbDir,callback);
            controlExecutor.execute(() -> handleSuccess(task, item));
        } catch (CancellationException e) {
            controlExecutor.execute(() -> handleCancelled(task));
        } catch (UploadDriveHelper.UploadFailure f) {
            controlExecutor.execute(() -> handleFailure(task, f.reason));
        } catch (Exception e) {
            controlExecutor.execute(() -> handleFailure(task, UploadDriveHelper.FailureReason.DRIVE_ERROR));
        }
    }


    private void handleSuccess(UploadTask task, UploadedItem uploadedItem) {
        if (current != task) return;
        clearRunning();
        if (callback != null) callback.onSuccess(task, uploadedItem);
        processQueue();
    }

    private void handleFailure(UploadTask task, UploadDriveHelper.FailureReason reason) {
        if (current != task) return;
        clearRunning();
        if (callback != null) callback.onFailure(task, reason);
        processQueue();
    }

    private void handleCancelled(UploadTask task) {
        if (current != task) return;
        clearRunning();
        if (callback != null) callback.onCancelled(task);
        processQueue();
    }

    void cancelGroup(String groupId) {
        queue.removeIf(t -> t.groupId.equals(groupId));
        if (current != null && current.groupId.equals(groupId)) {
            if (runningUpload != null) {
                runningUpload.cancel(true);
                runningUpload = null;
            }
            current = null;
        }
    }

    void cancelAll() {
        queue.clear();
        if (runningUpload != null) {
            runningUpload.cancel(true);
            runningUpload = null;
        }
        current = null;
        uploadDriveHelper.release();
    }

    private void clearRunning() {
        runningUpload = null;
        current = null;
    }
}
