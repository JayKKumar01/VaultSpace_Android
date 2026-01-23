package com.github.jaykkumar01.vaultspace.core.upload.helper;

import android.content.Context;
import android.util.Log;

import com.github.jaykkumar01.vaultspace.core.session.UploadRetryStore;
import com.github.jaykkumar01.vaultspace.core.session.cache.UploadCache;
import com.github.jaykkumar01.vaultspace.core.upload.base.FailureReason;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSnapshot;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class UploadFailureCoordinator {

    private static final String TAG = "VaultSpace:UploadManager";

    private final Context appContext;
    private final UploadCache uploadCache;
    private final UploadRetryStore retryStore;

    public UploadFailureCoordinator(Context appContext, UploadCache uploadCache, UploadRetryStore retryStore) {
        this.appContext = appContext;
        this.uploadCache = uploadCache;
        this.retryStore = retryStore;
    }



    public void recordRetriesIfMissing(String groupId, List<UploadSelection> selections) {
        List<UploadSelection> out = new ArrayList<>();
        for (UploadSelection s : selections)
            if (!retryStore.contains(groupId, s)) out.add(s);
        if (!out.isEmpty()) retryStore.addRetryBatch(groupId, out);
    }

    public void updateReason(String groupId, UploadSelection selection, FailureReason reason) {
        retryStore.updateFailureReason(groupId, selection, reason);
    }

    public List<UploadSelection> retry(String groupId, String groupName) {
        List<UploadSelection> all = retryStore.getAllRetries().get(groupId);
        if (all == null || all.isEmpty()) return null;

        uploadCache.removeSnapshot(groupId);

        int nonRetryable = 0, photos = 0, videos = 0, others = 0;
        List<UploadSelection> retryable = new ArrayList<>();

        for (UploadSelection s : all) {
            if (s.failureReason == null || s.failureReason.isRetryable()) retryable.add(s);
            else {
                nonRetryable++;
                switch (s.type) {
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

    public UploadSnapshot restoreFromRetry(String groupId, String groupName) {
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

            switch (s.type) {
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


    public void clearGroup(String groupId) {
        List<UploadSelection> rows = retryStore.getRetriesForGroup(groupId);
        for (UploadSelection e : rows) {
            if (e.thumbnailPath == null) continue;
            File f = new File(e.thumbnailPath);
            if (f.exists() && !f.delete())
                Log.w(TAG, "Failed to delete thumb: " + f.getAbsolutePath());
        }
        retryStore.clearGroup(groupId);
    }


}
