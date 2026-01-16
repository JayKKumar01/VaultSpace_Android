package com.github.jaykkumar01.vaultspace.album.helper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.jaykkumar01.vaultspace.album.upload.UploadSnapshot;
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
    public void onSnapshot(@Nullable UploadSnapshot snapshot) {

        if (snapshot == null) {
            statusView.hide();
            return;
        }

        /* ---------- Common data ---------- */

        statusView.setMediaCounts(
                snapshot.photos,
                snapshot.videos
        );

        statusView.setTotalCount(snapshot.total);
        statusView.setUploadedCount(snapshot.uploaded);
        statusView.setFailedCount(snapshot.failed);

        /* ---------- State resolution ---------- */

        if (snapshot.isInProgress()) {

            statusView.show();
            statusView.renderUploading(
                    v -> callback.onCancelRequested(),
                    snapshot.uploaded + snapshot.failed,
                    snapshot.total
            );

        } else if (snapshot.hasFailures()) {

            statusView.show();
            statusView.renderFailed(
                    v -> callback.onRetryRequested()
            );

        } else {

            statusView.show();
            statusView.renderCompleted(
                    v -> {
                        callback.onAcknowledge();
                        statusView.hide();
                    }
            );
        }
    }
}
