package com.github.jaykkumar01.vaultspace.core.download;

import static com.github.jaykkumar01.vaultspace.core.download.base.NotificationDetails.ACTION_START;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

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

    /* ================= Dependencies ================= */

    private final Context app;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final DownloadManager downloadManager;
    private final NotificationController notifier;

    /* ================= Constructor ================= */

    public DownloadOrchestrator(Context appContext) {
        this.app = appContext.getApplicationContext();
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
        notifier.dismiss();
        stopService();
    }

    /* ================= Service Callbacks ================= */

    @Override
    public synchronized void onServiceStarted() {
        serviceRunning = true;
        cancelled.set(false);
        maybeStartNext();
    }

    @Override
    public synchronized void onServiceDestroyed() {
        serviceRunning = false;
        cancelled.set(true);
        active = false;
        queue.clear();
        ids.clear();
        notifier.dismiss();
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

        notifier.showInitial(req.name);

        downloadManager.downloadAndFinalize(req, new DriveDownloadCallback() {
            @Override public void onProgress(long d, long t) {
                notifier.updateProgress(req.name, d, t);
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

        if (!cancelled.get()) {
            if (success) notifier.showCompleted(req.name);
            else notifier.showFailed(req.name);
        }

        maybeStartNext();
        finishIfIdle();
    }

    private synchronized void finishIfIdle() {
        if (active || !queue.isEmpty()) return;
        notifier.dismiss();
        stopService();
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
