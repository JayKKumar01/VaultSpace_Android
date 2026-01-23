package com.github.jaykkumar01.vaultspace.core.upload.controller;

import android.util.Log;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSnapshot;
import com.github.jaykkumar01.vaultspace.views.creative.upload.item.ProgressStackView;
import com.github.jaykkumar01.vaultspace.views.creative.upload.UploadStatusView;

/**
 * AlbumUploadStatusController
 *
 * Interprets UploadSnapshot and renders UploadStatusView.
 * Owns upload meaning, not upload execution.
 */
public final class UploadStatusController {

    /* ================= Listener ================= */

    public interface Callback {
        void onCancelRequested();
        void onRetryRequested();
        void onAcknowledge();
        void onNoAccessInfo();
    }

    /* ================= Fields ================= */

    private final UploadStatusView statusView;
    private final ProgressStackView progressStackView;

    private final Callback callback;

    /* ================= Constructor ================= */

    public UploadStatusController(
            @NonNull UploadStatusView statusView,
            @NonNull ProgressStackView progressStackView,
            @NonNull Callback callback
    ) {
        this.statusView = statusView;
        this.progressStackView = progressStackView;
        this.callback = callback;

        statusView.hide();
    }

    /* ================= Entry Point ================= */

    /**
     * Called repeatedly from UploadObserver.
     */
    public void onSnapshot(UploadSnapshot snapshot) {
        if (snapshot == null) {
            statusView.hide();
            return;
        }

        statusView.setMediaCounts(snapshot.photos, snapshot.videos);
        statusView.setUploadedCount(snapshot.uploaded);
        statusView.setNoAccessCount(snapshot.nonRetryableFailed);
        statusView.setFailedCount(snapshot.failed);
        statusView.setTotalCount(snapshot.total);

        if (snapshot.isInProgress()) {
            statusView.renderUploading(
                    v -> callback.onCancelRequested(),
                    snapshot.uploaded + snapshot.failed,
                    snapshot.total
            );
            return;
        }

        if (snapshot.hasRetryableFailures()) {
            statusView.renderFailed(
                    v -> callback.onRetryRequested()
            );
            return;
        }

        if (snapshot.hasOnlyNonRetryableFailures()) {
            statusView.renderNoAccess(v -> {
                statusView.hide();
                callback.onNoAccessInfo();
            });
            return;
        }

        statusView.renderCompleted(v -> {
            statusView.hide();
            callback.onAcknowledge();
        });
    }

    public void onProgress(String uId, String name, long uploadedBytes, long totalBytes) {
        Log.d(
                "VaultSpace:UploadProgress",
                "file=" + name +
                        " uploaded=" + uploadedBytes +
                        " total=" + totalBytes
        );
        progressStackView.render(uId,name, uploadedBytes, totalBytes);
    }




    public void onCancelled() {
        statusView.hide();
        progressStackView.reset();
    }
}
