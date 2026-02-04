package com.github.jaykkumar01.vaultspace.core.download;

import static com.github.jaykkumar01.vaultspace.core.download.base.NotificationDetails.ACTION_START;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.github.jaykkumar01.vaultspace.core.download.base.DownloadRequest;
import com.github.jaykkumar01.vaultspace.core.download.base.DownloadServiceCallbacks;
import com.github.jaykkumar01.vaultspace.core.download.base.DriveDownloadCallback;
import com.github.jaykkumar01.vaultspace.core.download.service.DownloadService;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiresApi(api = Build.VERSION_CODES.Q)
public final class DownloadOrchestrator implements DownloadServiceCallbacks {

    /* ================= State ================= */

    private final Queue<DownloadRequest> queue = new ArrayDeque<>();
    private final Set<String> ids = new HashSet<>();
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    private boolean active;
    private boolean serviceRunning;

    private int successCount;
    private int failureCount;
    private String lastSuccessName;

    /* ================= Dependencies ================= */

    private final Context app;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final DownloadManager downloadManager;
    private final NotificationController notifier;
    private final Handler main = new Handler(Looper.getMainLooper());

    /* ================= Constructor ================= */

    public DownloadOrchestrator(Context context) {
        this.app = context.getApplicationContext();
        this.downloadManager = new DownloadManager(app);
        this.notifier = new NotificationController(app);
    }

    /* ================= Public API ================= */

    public synchronized void enqueue(DownloadRequest req) {
        if (req == null || ids.contains(req.fileId)) return;
        queue.offer(req);
        ids.add(req.fileId);
        ensureService();
        maybeStartNext();
    }

    public synchronized void cancelAll() {
        cancelled.set(true);
        queue.clear();
        ids.clear();
        resetBatch();
        notifier.stopForeground();
        stopService();
    }

    /* ================= Service Callbacks ================= */

    @Override
    public synchronized void onServiceStarted() {
        serviceRunning = true;
        cancelled.set(false);

        successCount = 0;
        failureCount = 0;
        lastSuccessName = null;

        maybeStartNext();
    }

    @Override
    public synchronized void onServiceDestroyed() {
        serviceRunning = false;
        cancelled.set(true);
        active = false;

        queue.clear();
        ids.clear();
        resetBatch();

        notifier.stopForeground();
        executor.shutdownNow();
    }

    /* ================= Orchestration ================= */

    private synchronized void maybeStartNext() {
        if (!serviceRunning || active || queue.isEmpty()) return;
        active = true;
        executor.execute(this::runNext);
    }

    private void runNext() {
        DownloadRequest req;
        synchronized (this) { req = queue.poll(); }
        if (req == null) { finishIfIdle(); return; }

        notifier.startForeground(req.name, getPendingCount());

        downloadManager.downloadAndFinalize(req, new DriveDownloadCallback() {
            @Override public void onProgress(long d, long t) {
                notifier.updateProgress(req.name, d, t, getPendingCount());
            }
            @Override public void onCompleted() {
                onFinished(req, true);
            }
            @Override public void onFailed(Exception e) {
                onFinished(req, false);
            }
        }, cancelled);
    }

    private synchronized void onFinished(DownloadRequest req, boolean success) {
        ids.remove(req.fileId);
        active = false;

        if (success) {
            successCount++;
            lastSuccessName = req.name;
        } else {
            failureCount++;
        }

        maybeStartNext();
        finishIfIdle();
    }

    private synchronized void finishIfIdle() {
        if (active || !queue.isEmpty()) return;

        final int successSnapshot = successCount;
        final int failureSnapshot = failureCount;
        final String lastFileSnapshot = lastSuccessName;

        main.postDelayed(() -> {
            notifier.stopForeground();
            notifier.showCompletionSummary(
                    successSnapshot,
                    failureSnapshot,
                    lastFileSnapshot
            );
            notifier.reset();
        }, 800);

        stopService();
    }

    /* ================= Helpers ================= */

    private int getPendingCount() {
        return queue.size() + (active ? 1 : 0);
    }

    private void resetBatch() {
        successCount = 0;
        failureCount = 0;
        lastSuccessName = null;
        notifier.reset();
    }

    /* ================= Service Control ================= */

    private void ensureService() {
        if (serviceRunning) return;
        Intent i = new Intent(app, DownloadService.class);
        i.setAction(ACTION_START);
        ContextCompat.startForegroundService(app, i);
    }

    private void stopService() {
        app.stopService(new Intent(app, DownloadService.class));
    }
}
