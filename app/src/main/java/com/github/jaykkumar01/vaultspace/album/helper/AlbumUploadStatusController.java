package com.github.jaykkumar01.vaultspace.album.helper;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.upload.UploadSnapshot;
import com.github.jaykkumar01.vaultspace.views.creative.UploadStatusView;

/**
 * AlbumUploadStatusController
 *
 * Interprets UploadSnapshot and renders UploadStatusView.
 * Owns upload meaning, not upload execution.
 */
public final class AlbumUploadStatusController {

    /* ================= Callback ================= */

    public interface Callback {
        void onCancelRequested();
        void onRetryRequested();
        void onAcknowledge();
        void onNoAccessInfo();
    }

    /* ================= Fields ================= */

    private final UploadStatusView statusView;
    private final Callback callback;

    /* ================= Constructor ================= */

    public AlbumUploadStatusController(
            @NonNull UploadStatusView statusView,
            @NonNull Callback callback
    ) {
        this.statusView = statusView;
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
        statusView.setTotalCount(snapshot.total);
        statusView.setUploadedCount(snapshot.uploaded);
        statusView.setFailedCount(snapshot.failed);
        statusView.setNoAccessCount(snapshot.nonRetryableFailed);

        statusView.show();

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


    public void onCancelled() {
        statusView.hide();
    }
}
