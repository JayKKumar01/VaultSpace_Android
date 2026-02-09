package com.github.jaykkumar01.vaultspace.media.controller;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

public final class AttachPayload {

    public final DefaultMediaSourceFactory factory;
    public final MediaItem item;

    public AttachPayload(@NonNull DefaultMediaSourceFactory factory,
                         @NonNull MediaItem item) {
        this.factory = factory;
        this.item = item;
    }
}
