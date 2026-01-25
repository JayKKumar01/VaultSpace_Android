package com.github.jaykkumar01.vaultspace.album.listener;

import com.github.jaykkumar01.vaultspace.album.AlbumMedia;
import com.github.jaykkumar01.vaultspace.core.drive.AlbumMediaRepository;

import java.util.function.Consumer;

public final class AlbumMediaDeltaListenerImpl implements AlbumMediaRepository.MediaDeltaListener {

    private final Consumer<AlbumMedia> onAdded;
    private final Consumer<String> onRemoved;

    public AlbumMediaDeltaListenerImpl(
            Consumer<AlbumMedia> onAdded,
            Consumer<String> onRemoved
    ) {
        this.onAdded = onAdded;
        this.onRemoved = onRemoved;
    }

    @Override
    public void onMediaAdded(AlbumMedia media) {
        onAdded.accept(media);
    }

    @Override
    public void onMediaRemoved(String mediaId) {
        onRemoved.accept(mediaId);
    }
}
