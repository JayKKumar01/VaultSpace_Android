package com.github.jaykkumar01.vaultspace.core.upload;

import android.content.Context;

import com.github.jaykkumar01.vaultspace.core.session.UploadFailureStore;
import com.github.jaykkumar01.vaultspace.core.session.UploadRetryStore;
import com.github.jaykkumar01.vaultspace.core.session.cache.UploadCache;
import com.github.jaykkumar01.vaultspace.models.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.utils.UriUtils;

import java.util.List;

final class UploadSnapshotReducer {

    private final Context appContext;
    private final UploadCache uploadCache;
    private final UploadRetryStore retryStore;
    private final UploadFailureStore failureStore;

    UploadSnapshotReducer(Context appContext,UploadCache uploadCache,UploadRetryStore retryStore,UploadFailureStore failureStore) {
        this.appContext = appContext;
        this.uploadCache = uploadCache;
        this.retryStore = retryStore;
        this.failureStore = failureStore;
    }

    UploadSnapshot mergeSnapshot(String groupId,String groupName,List<UploadSelection> selections) {
        int photos = 0, videos = 0, others = 0;
        for (UploadSelection s : selections) {
            switch (s.getType()) {
                case PHOTO -> photos++;
                case VIDEO -> videos++;
                case FILE -> others++;
            }
        }

        UploadSnapshot old = uploadCache.getSnapshot(groupId);
        int uploaded = 0, failed = 0;
        if (old != null) {
            photos += old.photos;
            videos += old.videos;
            others += old.others;
            uploaded = old.uploaded;
            failed = old.failed;
        }

        return new UploadSnapshot(groupId, groupName, photos, videos, others, uploaded, failed);
    }

    UploadSnapshot onSuccess(UploadTask task) {
        UploadSnapshot old = uploadCache.getSnapshot(task.groupId);
        if (old == null) return null;

        retryStore.removeRetry(task.groupId, task.selection);
        failureStore.removeFailure(task.groupId, task.selection.uri.toString(), task.selection.getType().name());

        UploadSnapshot updated = new UploadSnapshot(
                old.groupId, old.groupName,
                old.photos, old.videos, old.others,
                old.uploaded + 1, old.failed
        );
        updated.nonRetryableFailed = old.nonRetryableFailed;
        return updated;
    }

    UploadSnapshot onFailure(UploadTask task) {
        UploadSnapshot old = uploadCache.getSnapshot(task.groupId);
        if (old == null) return null;

        boolean retryable = UriUtils.isUriAccessible(appContext, task.selection.uri);

        UploadSnapshot updated = new UploadSnapshot(
                old.groupId, old.groupName,
                old.photos, old.videos, old.others,
                old.uploaded, old.failed + 1
        );
        updated.nonRetryableFailed = old.nonRetryableFailed;
        if (!retryable) updated.nonRetryableFailed++;
        return updated;
    }

    void finalizeSnapshot(UploadSnapshot snapshot) {
        uploadCache.putSnapshot(snapshot);
    }
}
