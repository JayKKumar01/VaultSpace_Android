package com.github.jaykkumar01.vaultspace.core.upload.helper;

import android.content.Context;

import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.core.upload.drive.UploadDriveHelper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class UploadDispatcher {
    private static final int MAX_PARALLEL = 3;
    private final ExecutorService executor = Executors.newFixedThreadPool(MAX_PARALLEL);
    private final ConcurrentLinkedQueue<UploadTask> queue = new ConcurrentLinkedQueue<>();
    private final Map<String, Set<Future<?>>> runningByGroup = new ConcurrentHashMap<>();
    private final AtomicInteger running = new AtomicInteger();
    private final UploadDriveHelper driveHelper;

    public UploadDispatcher(Context context) {
        this.driveHelper = new UploadDriveHelper(context);
    }

    public void enqueue(List<UploadSelection> selections, UploadTask.Callback callback) {
        if (selections == null || selections.isEmpty()) return;
        for (UploadSelection s : selections) {
            queue.add(new UploadTask(s, driveHelper, callback));
        }
        trySchedule();
    }

    private void trySchedule() {
        synchronized (this) {
            while (running.get() < MAX_PARALLEL) {
                UploadTask task = queue.poll();
                if (task == null) return;
                running.incrementAndGet();
                final Future<?>[] ref = new Future<?>[1];
                ref[0] = executor.submit(() -> {
                    try {
                        task.run();
                    } finally {
                        onTaskFinished(task.groupId, ref[0]);
                    }
                });
                runningByGroup.computeIfAbsent(task.groupId, k -> ConcurrentHashMap.newKeySet()).add(ref[0]);
            }
        }
    }

    private void onTaskFinished(String groupId, Future<?> future) {
        running.decrementAndGet();
        cleanup(groupId, future);
        trySchedule();
    }

    public void cancelGroup(String groupId) {
        Set<Future<?>> set = runningByGroup.remove(groupId);
        if (set != null) for (Future<?> f : set) f.cancel(true);
        queue.removeIf(t -> t.groupId.equals(groupId));
    }

    public void cancelAll() {
        for (Set<Future<?>> set : runningByGroup.values())
            for (Future<?> f : set) f.cancel(true);
        runningByGroup.clear();
        queue.clear();
    }

    public int getActiveCount() {
        return running.get();
    }

    public void shutdown() {
        cancelAll();
        executor.shutdownNow();
    }

    private void cleanup(String groupId, Future<?> future) {
        Set<Future<?>> set = runningByGroup.get(groupId);
        if (set != null) {
            set.remove(future);
            if (set.isEmpty()) runningByGroup.remove(groupId);
        }
    }
}
