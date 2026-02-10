package com.github.jaykkumar01.vaultspace.media.controller;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

public final class AttachPayload {
    public final DefaultMediaSourceFactory mediaSourceFactory;
    public final  CacheDataSource.Factory dataSourceFactory;
    public final MediaItem mediaItem;

    public AttachPayload(
            DefaultMediaSourceFactory mediaSourceFactory,
            CacheDataSource.Factory dataSourceFactory,
            MediaItem mediaItem
    ) {
        this.mediaSourceFactory = mediaSourceFactory;
        this.dataSourceFactory = dataSourceFactory;
        this.mediaItem = mediaItem;
    }
}

