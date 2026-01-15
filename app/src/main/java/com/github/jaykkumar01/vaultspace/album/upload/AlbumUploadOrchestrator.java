package com.github.jaykkumar01.vaultspace.album.upload;

import android.telecom.Call;

import com.github.jaykkumar01.vaultspace.models.MediaSelection;
import com.github.jaykkumar01.vaultspace.views.creative.UploadStatusView;

import java.util.ArrayList;
import java.util.List;

public class AlbumUploadOrchestrator {

    public interface Callback {

        /**
         * Called for every upload state / progress change.
         */
        void onStateChanged(UploadSnapshot snapshot);

        /**
         * Called once when upload reaches a terminal state.
         *
         * @param hadFailures true if at least one item failed
         */
        void onCompleted(boolean hadFailures);
    }

    private final String albumId;
    private final Callback callback;

    private UploadSnapshot snapshot =
            new UploadSnapshot(
                    0,
                    0,
                    0,
                    0,
                    0,
                    UploadStatusView.State.UPLOADING
            );

    public AlbumUploadOrchestrator(String albumId, Callback callback) {
        this.albumId = albumId;
        this.callback = callback;
    }

    /* ================= Public API ================= */

    public void startUpload(List<MediaSelection> selections) {
        if (selections == null || selections.isEmpty()) return;

        int photos = 0;
        int videos = 0;

        for (MediaSelection s : selections) {
            if (s.isVideo) videos++;
            else photos++;
        }

        snapshot = new UploadSnapshot(
                photos,
                videos,
                selections.size(),
                0,
                0,
                UploadStatusView.State.UPLOADING
        );

        callback.onStateChanged(snapshot);

        //
    }

    public void cancelUpload() {
        // TODO
    }
}
