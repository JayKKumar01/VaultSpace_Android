package com.github.jaykkumar01.vaultspace.album.listener;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.core.drive.AlbumMediaRepository;

import java.util.function.Consumer;

public final class AlbumStateListenerImpl
        implements AlbumMediaRepository.AlbumStateListener {

    private final Runnable onLoading;
    private final Consumer<Iterable<AlbumMedia>> onMedia;
    private final Consumer<Exception> onError;

    public AlbumStateListenerImpl(
            Runnable onLoading,
            Consumer<Iterable<AlbumMedia>> onMedia,
            Consumer<Exception> onError
    ) {
        this.onLoading = onLoading;
        this.onMedia = onMedia;
        this.onError = onError;
    }

    @Override public void onLoading() { onLoading.run(); }
    @Override public void onMedia(Iterable<AlbumMedia> media) { onMedia.accept(media); }
    @Override public void onError(Exception e) { onError.accept(e); }
}
