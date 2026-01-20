package com.github.jaykkumar01.vaultspace.core.upload;

import com.github.jaykkumar01.vaultspace.models.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.models.base.UploadedItem;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

final class UploadQueueEngine {

    interface Callback {
        void onSuccess(UploadTask task, UploadedItem uploadedItem);
        void onFailure(UploadTask task);
        void onCancelled(UploadTask task);
        void onIdle();
    }

    private final ExecutorService controlExecutor;
    private final ExecutorService uploadExecutor;
    private final Deque<UploadTask> queue = new ArrayDeque<>();

    private UploadTask current;
    private Future<?> runningUpload;
    private Callback callback;

    UploadQueueEngine(ExecutorService controlExecutor,ExecutorService uploadExecutor) {
        this.controlExecutor = controlExecutor;
        this.uploadExecutor = uploadExecutor;
    }

    void setCallback(Callback callback) {
        this.callback = callback;
    }

    void enqueue(String groupId,List<UploadSelection> selections) {
        for (UploadSelection s : selections)
            queue.add(new UploadTask(groupId, s));
    }

    void processQueue() {
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
            Thread.sleep(2000);
            if (Math.random() > 0.5)
                controlExecutor.execute(() -> handleSuccess(task,new UploadedItem("fileId","name","mimeType",0, 0,"")));
            else
                controlExecutor.execute(() -> handleFailure(task));
        } catch (InterruptedException e) {
            controlExecutor.execute(() -> handleCancelled(task));
        }
    }

    private void handleSuccess(UploadTask task, UploadedItem uploadedItem) {
        if (current != task) return;
        clearRunning();
        if (callback != null) callback.onSuccess(task,uploadedItem);
        processQueue();
    }

    private void handleFailure(UploadTask task) {
        if (current != task) return;
        clearRunning();
        if (callback != null) callback.onFailure(task);
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
    }

    private void clearRunning() {
        runningUpload = null;
        current = null;
    }
}
