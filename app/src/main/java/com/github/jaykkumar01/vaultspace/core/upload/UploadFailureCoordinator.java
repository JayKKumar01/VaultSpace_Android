package com.github.jaykkumar01.vaultspace.core.upload;

import android.content.Context;
import android.util.Log;

import com.github.jaykkumar01.vaultspace.core.session.UploadFailureStore;
import com.github.jaykkumar01.vaultspace.core.session.UploadRetryStore;
import com.github.jaykkumar01.vaultspace.core.session.cache.UploadCache;
import com.github.jaykkumar01.vaultspace.core.session.db.UploadFailureEntity;
import com.github.jaykkumar01.vaultspace.core.session.db.UploadFailureReason;
import com.github.jaykkumar01.vaultspace.core.upload.drive.UploadDriveHelper;
import com.github.jaykkumar01.vaultspace.models.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.utils.UploadMetadataResolver;
import com.github.jaykkumar01.vaultspace.utils.UploadThumbnailGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

final class UploadFailureCoordinator {

    private static final String TAG = "VaultSpace:UploadManager";

    private final Context appContext;
    private final UploadCache uploadCache;
    private final UploadRetryStore retryStore;
    private final UploadFailureStore failureStore;
    private final File thumbDir;

    UploadFailureCoordinator(Context appContext, UploadCache uploadCache, UploadRetryStore retryStore, UploadFailureStore failureStore, File thumbDir) {
        this.appContext = appContext;
        this.uploadCache = uploadCache;
        this.retryStore = retryStore;
        this.failureStore = failureStore;
        this.thumbDir = thumbDir;
    }

    void recordFailuresIfMissingAsync(String groupId, List<UploadSelection> selections, ExecutorService thumbExecutor) {
        for (UploadSelection s : selections) {
            if (failureStore.contains(groupId, s.uri.toString(), s.getType().name())) continue;
            thumbExecutor.execute(() -> {
                String name = UploadMetadataResolver.resolveDisplayName(appContext, s.uri);
                String thumb = UploadThumbnailGenerator.generate(appContext, s.uri, s.getType(), thumbDir);
                failureStore.addFailure(new UploadFailureEntity(
                        0L, groupId, s.uri.toString(),
                        name, s.getType().name(),
                        thumb, UploadFailureReason.UNKNOWN.name()
                ));
            });
        }
    }


    void recordRetriesIfMissing(String groupId, List<UploadSelection> selections) {
        List<UploadSelection> out = new ArrayList<>();
        for (UploadSelection s : selections)
            if (!retryStore.contains(groupId, s)) out.add(s);
        if (!out.isEmpty()) retryStore.addRetryBatch(groupId, out);
    }

    public void updateReason(UploadTask task, UploadDriveHelper.FailureReason reason) {
        retryStore.updateFailureReason(task.groupId, task.selection, reason);
    }

    List<UploadSelection> retry(String groupId, String groupName) {
        List<UploadSelection> all = retryStore.getAllRetries().get(groupId);
        if (all == null || all.isEmpty()) return null;

        uploadCache.removeSnapshot(groupId);

        int nonRetryable = 0, photos = 0, videos = 0, others = 0;
        List<UploadSelection> retryable = new ArrayList<>();

        for (UploadSelection s : all) {
            if (s.failureReason == null || s.failureReason.isRetryable()) retryable.add(s);
            else {
                nonRetryable++;
                switch (s.getType()) {
                    case PHOTO -> photos++;
                    case VIDEO -> videos++;
                    case FILE -> others++;
                }
            }
        }


        if (nonRetryable > 0) {
            UploadSnapshot snapshot = new UploadSnapshot(groupId, groupName, photos, videos, others, 0, nonRetryable);
            snapshot.nonRetryableFailed = nonRetryable;
            uploadCache.putSnapshot(snapshot);
        }

        return retryable;
    }

    UploadSnapshot restoreFromRetry(String groupId, String groupName) {
        List<UploadSelection> list = retryStore.getAllRetries().get(groupId);
        if (list == null || list.isEmpty()) {
            Log.d(TAG, "restoreFromRetry: no retries for group=" + groupId);
            return null;
        }

        int nonRetryable = 0, photos = 0, videos = 0, others = 0;

        for (UploadSelection s : list) {
            boolean accessible = s.failureReason == null || s.failureReason.isRetryable();
            if (!accessible) {
                nonRetryable++;
                Log.d(TAG, "restoreFromRetry: NON-retryable uri=" + s.uri);
            }

            switch (s.getType()) {
                case PHOTO -> photos++;
                case VIDEO -> videos++;
                case FILE -> others++;
            }
        }

        Log.d(TAG,
                "restoreFromRetry: group=" + groupId +
                        " total=" + list.size() +
                        " photos=" + photos +
                        " videos=" + videos +
                        " files=" + others +
                        " nonRetryable=" + nonRetryable
        );

        UploadSnapshot snapshot =
                new UploadSnapshot(groupId, groupName, photos, videos, others, 0, photos + videos + others);
        snapshot.nonRetryableFailed = nonRetryable;

        uploadCache.putSnapshot(snapshot);
        Log.d(TAG, "restoreFromRetry: snapshot restored & cached for group=" + groupId);

        return snapshot;
    }


    void clearGroup(String groupId) {
        List<UploadFailureEntity> rows = failureStore.getFailuresForGroup(groupId);
        if (rows != null) {
            for (UploadFailureEntity e : rows) {
                if (e.thumbnailPath == null) continue;
                File f = new File(e.thumbnailPath);
                if (f.exists() && !f.delete())
                    Log.w(TAG, "Failed to delete thumb: " + f.getAbsolutePath());
            }
        }
        failureStore.clearGroup(groupId);
        retryStore.clearGroup(groupId);
    }


}
