package com.github.jaykkumar01.vaultspace.core.upload;

import android.content.Context;
import android.util.Log;

import com.github.jaykkumar01.vaultspace.core.session.UploadFailureStore;
import com.github.jaykkumar01.vaultspace.core.session.UploadRetryStore;
import com.github.jaykkumar01.vaultspace.core.session.cache.UploadCache;
import com.github.jaykkumar01.vaultspace.core.session.db.UploadFailureEntity;
import com.github.jaykkumar01.vaultspace.core.session.db.UploadFailureReason;
import com.github.jaykkumar01.vaultspace.models.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.utils.UploadMetadataResolver;
import com.github.jaykkumar01.vaultspace.utils.UploadThumbnailGenerator;
import com.github.jaykkumar01.vaultspace.utils.UriUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class UploadFailureCoordinator {

    private static final String TAG = "VaultSpace:UploadManager";

    private final Context appContext;
    private final UploadCache uploadCache;
    private final UploadRetryStore retryStore;
    private final UploadFailureStore failureStore;
    private final File thumbDir;

    UploadFailureCoordinator(Context appContext,UploadCache uploadCache,UploadRetryStore retryStore,UploadFailureStore failureStore,File thumbDir) {
        this.appContext = appContext;
        this.uploadCache = uploadCache;
        this.retryStore = retryStore;
        this.failureStore = failureStore;
        this.thumbDir = thumbDir;
    }

    void recordFailuresIfMissing(String groupId,List<UploadSelection> selections) {
        List<UploadFailureEntity> out = new ArrayList<>();
        for (UploadSelection s : selections) {
            if (failureStore.contains(groupId, s.uri.toString(), s.getType().name())) continue;
            String name = UploadMetadataResolver.resolveDisplayName(appContext, s.uri);
            String thumb = UploadThumbnailGenerator.generate(appContext, s.uri, s.getType(), thumbDir);
            out.add(new UploadFailureEntity(
                    0L, groupId, s.uri.toString(),
                    name, s.getType().name(),
                    thumb, UploadFailureReason.UNKNOWN.name()
            ));
        }
        if (!out.isEmpty()) failureStore.addFailures(out);
    }

    void recordRetriesIfMissing(String groupId,List<UploadSelection> selections) {
        List<UploadSelection> out = new ArrayList<>();
        for (UploadSelection s : selections)
            if (!retryStore.contains(groupId, s)) out.add(s);
        if (!out.isEmpty()) retryStore.addRetryBatch(groupId, out);
    }

    List<UploadSelection> retry(String groupId,String groupName) {
        List<UploadSelection> all = retryStore.getAllRetries().get(groupId);
        if (all == null || all.isEmpty()) return null;

        uploadCache.removeSnapshot(groupId);

        int nonRetryable = 0, photos = 0, videos = 0, others = 0;
        List<UploadSelection> retryable = new ArrayList<>();

        for (UploadSelection s : all) {
            boolean accessible = UriUtils.isUriAccessible(appContext, s.uri);
            if (accessible) retryable.add(s);
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

    UploadSnapshot restoreFromRetry(String groupId,String groupName) {
        List<UploadSelection> list = retryStore.getAllRetries().get(groupId);
        if (list == null || list.isEmpty()) return null;

        int nonRetryable = 0, photos = 0, videos = 0, others = 0;
        for (UploadSelection s : list) {
            if (!UriUtils.isUriAccessible(appContext, s.uri)) nonRetryable++;
            switch (s.getType()) {
                case PHOTO -> photos++;
                case VIDEO -> videos++;
                case FILE -> others++;
            }
        }

        UploadSnapshot snapshot = new UploadSnapshot(groupId, groupName, photos, videos, others, 0, photos + videos + others);
        snapshot.nonRetryableFailed = nonRetryable;
        uploadCache.putSnapshot(snapshot);
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
