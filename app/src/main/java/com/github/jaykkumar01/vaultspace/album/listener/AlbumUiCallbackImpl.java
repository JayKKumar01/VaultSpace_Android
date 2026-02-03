package com.github.jaykkumar01.vaultspace.album.listener;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.album.helper.AlbumUiController;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class AlbumUiCallbackImpl implements AlbumUiController.Callback {

    private final Runnable onAdd;
    private final Consumer<AlbumMedia> onClick;
    private final Consumer<AlbumMedia> onLongClick;

    public AlbumUiCallbackImpl(
            Runnable onAdd,
            Consumer<AlbumMedia> onClick,
            Consumer<AlbumMedia> onLongClick
    ) {
        this.onAdd = onAdd;
        this.onClick = onClick;
        this.onLongClick = onLongClick;
    }

    @Override public void onAddMediaClicked() { onAdd.run(); }
    @Override public void onMediaClicked(AlbumMedia m) { onClick.accept(m); }
    @Override public void onMediaLongPressed(AlbumMedia m) { onLongClick.accept(m); }
}
