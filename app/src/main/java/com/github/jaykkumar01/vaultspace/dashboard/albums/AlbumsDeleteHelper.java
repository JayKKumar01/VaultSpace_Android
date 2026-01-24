package com.github.jaykkumar01.vaultspace.dashboard.albums;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.github.jaykkumar01.vaultspace.core.drive.TrustedAccountsRepository;
import com.github.jaykkumar01.vaultspace.views.creative.delete.DeleteProgressCallback;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class AlbumsDeleteHelper {

    private static final int MAX_PARALLEL = 3;
    private static final int PAGE_SIZE = 200;
    private static final String PRIMARY = "__primary__";

    private final Context appContext;
    private final TrustedAccountsRepository trustedRepo;
    private final Drive primaryDrive;
    private final ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Queue<AccountDeleteUnit> queue = new ConcurrentLinkedQueue<>();
    private final Map<String, Drive> driveCache = new ConcurrentHashMap<>();

    private final AtomicInteger running = new AtomicInteger(0);
    private final AtomicInteger done = new AtomicInteger(0);
    private final AtomicInteger total = new AtomicInteger(0);

    private volatile boolean finished = false;

    public AlbumsDeleteHelper(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.trustedRepo = TrustedAccountsRepository.getInstance(context);
        this.primaryDrive = DriveClientProvider.getPrimaryDrive(appContext);
        this.executor = Executors.newFixedThreadPool(MAX_PARALLEL);
    }

    public void deleteAlbum(@NonNull String albumId,
                            @NonNull DeleteProgressCallback cb,
                            @NonNull AtomicBoolean cancelled) {

        post(() -> cb.onStart(0));

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                discover(albumId, cb, cancelled);
                if (!finished) schedule(albumId, cb, cancelled);
            } catch (Exception e) {
                failOnce(cb, e);
            }
        });
    }

    public void shutdown() {
        executor.shutdownNow();
        queue.clear();
        driveCache.clear();
    }

    private void discover(String albumId,
                          DeleteProgressCallback cb,
                          AtomicBoolean cancelled) throws Exception {

        Map<String, List<File>> byOwner = new HashMap<>();
        String pageToken = null;

        do {
            FileList list = primaryDrive.files().list()
                    .setQ("'" + albumId + "' in parents and trashed=false")
                    .setFields("nextPageToken,files(id,name,size,owners(emailAddress),appProperties)")
                    .setPageSize(PAGE_SIZE)
                    .setPageToken(pageToken)
                    .execute();

            if (list.getFiles() != null) {
                for (File f : list.getFiles()) {
                    if (cancelled.get()) throw new Exception("Delete cancelled");

                    String owner = (f.getOwners() != null && !f.getOwners().isEmpty())
                            ? f.getOwners().get(0).getEmailAddress()
                            : PRIMARY;

                    byOwner.computeIfAbsent(owner, k -> new ArrayList<>()).add(f);
                    total.incrementAndGet();
                }
            }

            pageToken = list.getNextPageToken();
        } while (pageToken != null);

        if (total.get() == 0) {
            if (cancelled.get()) throw new Exception("Delete cancelled");
            primaryDrive.files().delete(albumId).execute();
            finished = true;
            post(cb::onCompleted);
            return;
        }

        post(() -> cb.onStart(total.get()));

        for (Map.Entry<String, List<File>> e : byOwner.entrySet()) {
            queue.add(new AccountDeleteUnit(e.getKey(), e.getValue()));
        }
    }

    private void schedule(String albumId,
                          DeleteProgressCallback cb,
                          AtomicBoolean cancelled) {
        synchronized (this) {
            while (!finished && !cancelled.get() && running.get() < MAX_PARALLEL) {
                AccountDeleteUnit unit = queue.poll();
                if (unit == null) return;
                running.incrementAndGet();
                executor.execute(() -> runUnit(unit, albumId, cb, cancelled));
            }
        }
    }

    private void runUnit(AccountDeleteUnit unit,
                         String albumId,
                         DeleteProgressCallback cb,
                         AtomicBoolean cancelled) {
        try {
            Drive drive = resolveDrive(unit.owner);
            for (File f : unit.files) {
                if (cancelled.get()) throw new Exception("Delete cancelled");

                deleteThumbIfAny(drive, f);
                drive.files().delete(f.getId()).execute();

                int d = done.incrementAndGet();
                post(() -> cb.onFileDeleting(f.getName(), d, total.get()));

                if (f.getSize() != null && !PRIMARY.equals(unit.owner)) {
                    trustedRepo.recordDeleteUsage(unit.owner, f.getSize(), null);
                }
            }
            onUnitFinished(albumId, cb, cancelled);
        } catch (Exception e) {
            failOnce(cb, e);
        }
    }

    private void onUnitFinished(String albumId,
                                DeleteProgressCallback cb,
                                AtomicBoolean cancelled) {

        running.decrementAndGet();

        if (cancelled.get()) {
            failOnce(cb, new Exception("Delete cancelled"));
            return;
        }

        if (queue.isEmpty() && running.get() == 0 && !finished) {
            finished = true;
            try {
                primaryDrive.files().delete(albumId).execute();
                post(cb::onCompleted);
            } catch (Exception e) {
                failOnce(cb, e);
            }
        } else {
            schedule(albumId, cb, cancelled);
        }
    }

    private Drive resolveDrive(String owner) {
        if (PRIMARY.equals(owner)) return primaryDrive;
        return driveCache.computeIfAbsent(owner,
                k -> DriveClientProvider.forAccount(appContext, k));
    }

    private void deleteThumbIfAny(Drive drive, File f) {
        try {
            Map<String, String> props = f.getAppProperties();
            if (props != null && props.containsKey("thumb")) {
                drive.files().delete(props.get("thumb")).execute();
            }
        } catch (Exception ignored) {}
    }

    private void failOnce(DeleteProgressCallback cb, Exception e) {
        if (finished) return;
        finished = true;
        post(() -> cb.onError(e));
    }

    private void post(Runnable r) {
        mainHandler.post(r);
    }

    private record AccountDeleteUnit(String owner, List<File> files) {}
}
