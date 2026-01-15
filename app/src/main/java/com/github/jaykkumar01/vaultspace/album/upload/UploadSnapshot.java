package com.github.jaykkumar01.vaultspace.album.upload;

import com.github.jaykkumar01.vaultspace.views.creative.UploadStatusView;

public final class UploadSnapshot {

    public final int photos;
    public final int videos;

    public final int total;
    public final int uploaded;
    public final int failed;

    public final UploadStatusView.State state;

    public UploadSnapshot(
            int photos,
            int videos,
            int total,
            int uploaded,
            int failed,
            UploadStatusView.State state
    ) {
        this.photos = photos;
        this.videos = videos;
        this.total = total;
        this.uploaded = uploaded;
        this.failed = failed;
        this.state = state;
    }

    public boolean isIdle() {
        return total == 0;
    }

    public boolean isFinished() {
        return state == UploadStatusView.State.COMPLETED
                || state == UploadStatusView.State.FAILED;
    }
}
