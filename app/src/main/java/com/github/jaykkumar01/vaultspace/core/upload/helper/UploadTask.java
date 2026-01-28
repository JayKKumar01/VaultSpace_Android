package com.github.jaykkumar01.vaultspace.core.upload.helper;

import com.github.jaykkumar01.vaultspace.core.upload.base.*;
import com.github.jaykkumar01.vaultspace.core.upload.drive.UploadDriveHelper;

import java.util.concurrent.CancellationException;

public final class UploadTask implements Runnable {

    public interface Callback {
        void onSuccess(String groupId, UploadSelection s, UploadedItem item);
        void onFailure(String groupId, UploadSelection s, FailureReason r);
        void onProgress(String uploadId, String groupId, UploadSelection selection, long uploaded, long total);
    }

    final String uploadId;
    final String groupId;
    final UploadSelection selection;

    private final UploadDriveHelper helper;
    private final Callback cb;
    private final CancelToken cancelToken;

    public UploadTask(
            UploadSelection s,
            UploadDriveHelper h,
            Callback cb,
            UploadDispatcher dispatcher,
            int generation
    ) {
        this.uploadId = s.id;
        this.groupId = s.context.groupId;
        this.selection = s;
        this.helper = h;
        this.cb = cb;
        this.cancelToken = () ->
                dispatcher.currentGeneration(groupId) != generation;
    }

    @Override
    public void run() {
        if (Thread.currentThread().isInterrupted()) return;

        try {
            UploadedItem item = helper.upload(
                    groupId,
                    selection,
                    (u, t) -> {
                        if (cancelToken.isCancelled())
                            throw new CancellationException();
                        cb.onProgress(uploadId, groupId, selection, u, t);
                    },
                    cancelToken
            );

            if (cancelToken.isCancelled()) return;
            cb.onSuccess(groupId, selection, item);

        } catch (UploadDriveHelper.UploadFailure f) {
            if (cancelToken.isCancelled()) return;
            cb.onFailure(groupId, selection, f.reason);

        } catch (CancellationException ignored) {
            // expected on cancel

        } catch (Exception e) {
            if (Thread.currentThread().isInterrupted()) return;
            throw e;
        }
    }
}
