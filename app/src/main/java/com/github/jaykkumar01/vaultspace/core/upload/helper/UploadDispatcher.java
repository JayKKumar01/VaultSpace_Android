package com.github.jaykkumar01.vaultspace.core.upload.helper;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class UploadDispatcher {

    private static final String TAG = "VaultSpace:Dispatcher";
    private static final int MAX_PARALLEL = 3;

    private final ExecutorService executor = Executors.newFixedThreadPool(MAX_PARALLEL);
    private final ConcurrentLinkedQueue<DispatchItem> queue = new ConcurrentLinkedQueue<>();
    private final Map<String, Set<Future<?>>> runningByGroup = new ConcurrentHashMap<>();
    private final AtomicInteger running = new AtomicInteger();

    public void submit(String groupId, Runnable task) {
        queue.add(new DispatchItem(groupId, task));
        trySchedule();
    }

    private void trySchedule() {
        synchronized (this) {
            while (running.get() < MAX_PARALLEL) {
                DispatchItem item = queue.poll();
                if (item == null) return;
                running.incrementAndGet();
                final Future<?>[] ref = new Future<?>[1];
                ref[0] = executor.submit(() -> {
                    try {
                        item.task.run();
                    } finally {
                        onTaskFinished(item.groupId, ref[0]);
                    }
                });
                runningByGroup.computeIfAbsent(item.groupId, k -> ConcurrentHashMap.newKeySet()).add(ref[0]);
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
        queue.removeIf(i -> i.groupId.equals(groupId));
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

    private record DispatchItem(String groupId, Runnable task) {
    }
}
