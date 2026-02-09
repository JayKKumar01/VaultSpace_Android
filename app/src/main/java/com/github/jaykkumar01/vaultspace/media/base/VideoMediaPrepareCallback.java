package com.github.jaykkumar01.vaultspace.media.base;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

public interface VideoMediaPrepareCallback {
    void onReady(@NonNull DefaultMediaSourceFactory factory,
                 @NonNull MediaItem item);
    void onError(@NonNull Exception e);

    /**
     * OPTIONAL.
     * Only called by download-based sources.
     */
    default void onProgress(long bytesDownloaded, long totalBytes) {
        // no-op
    }
}
