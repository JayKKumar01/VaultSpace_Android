package com.github.jaykkumar01.vaultspace.media.source;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

public interface MediaSourceCallback {

    void onReady(@NonNull DefaultMediaSourceFactory factory,
                 @NonNull MediaItem item);

    void onFailure(@NonNull Exception e);
}
