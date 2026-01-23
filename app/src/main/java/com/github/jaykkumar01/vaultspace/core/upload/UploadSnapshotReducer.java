package com.github.jaykkumar01.vaultspace.core.upload;

import android.content.Context;
import com.github.jaykkumar01.vaultspace.core.session.UploadRetryStore;
import com.github.jaykkumar01.vaultspace.core.session.cache.UploadCache;
import com.github.jaykkumar01.vaultspace.core.upload.base.FailureReason;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSelection;

import java.util.List;

final class UploadSnapshotReducer {

    private final Context appContext;
    private final UploadCache uploadCache;
    private final UploadRetryStore retryStore;

    UploadSnapshotReducer(Context appContext,UploadCache uploadCache,UploadRetryStore retryStore) {
        this.appContext = appContext;
        this.uploadCache = uploadCache;
        this.retryStore = retryStore;
    }

    UploadSnapshot mergeSnapshot(String groupId,String groupName,List<UploadSelection> selections) {
        int photos = 0, videos = 0, others = 0;
        for (UploadSelection s : selections) {
            switch (s.type) {
                case PHOTO -> photos++;
                case VIDEO -> videos++;
                case FILE -> others++;
            }
        }

        UploadSnapshot old = uploadCache.getSnapshot(groupId);
        int uploaded = 0, failed = 0, nonRetryableFailed = 0;
        if (old != null) {
            photos += old.photos;
            videos += old.videos;
            others += old.others;
            uploaded = old.uploaded;
            failed = old.failed;
            nonRetryableFailed = old.nonRetryableFailed;
        }

        UploadSnapshot snapshot = new UploadSnapshot(groupId, groupName, photos, videos, others, uploaded, failed);
        snapshot.nonRetryableFailed = nonRetryableFailed;
        return snapshot;
    }

    UploadSnapshot onSuccess(String groupId, UploadSelection selection) {
        UploadSnapshot old = uploadCache.getSnapshot(groupId);
        if (old == null) return null;

        retryStore.removeRetry(groupId, selection);

        UploadSnapshot updated = new UploadSnapshot(
                old.groupId, old.groupName,
                old.photos, old.videos, old.others,
                old.uploaded + 1, old.failed
        );
        updated.nonRetryableFailed = old.nonRetryableFailed;
        return updated;
    }


    UploadSnapshot onFailure(String groupId, FailureReason reason) {
        UploadSnapshot old = uploadCache.getSnapshot(groupId);
        if (old == null) return null;


        UploadSnapshot updated = new UploadSnapshot(
                old.groupId, old.groupName,
                old.photos, old.videos, old.others,
                old.uploaded, old.failed + 1
        );
        updated.nonRetryableFailed = old.nonRetryableFailed;
        if (!reason.isRetryable()) updated.nonRetryableFailed++;
        return updated;
    }

    void finalizeSnapshot(UploadSnapshot snapshot) {
        uploadCache.putSnapshot(snapshot);
    }
}
