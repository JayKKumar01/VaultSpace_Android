package com.github.jaykkumar01.vaultspace.album.listener;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.album.helper.AlbumUiController;

import java.util.function.BiConsumer;

public final class AlbumUiCallbackImpl implements AlbumUiController.Callback {

    private final Runnable onAdd;
    private final BiConsumer<AlbumMedia, Integer> onClick;
    private final BiConsumer<AlbumMedia, Integer> onLongClick;

    public AlbumUiCallbackImpl(
            Runnable onAdd,
            BiConsumer<AlbumMedia, Integer> onClick,
            BiConsumer<AlbumMedia, Integer> onLongClick
    ) {
        this.onAdd = onAdd;
        this.onClick = onClick;
        this.onLongClick = onLongClick;
    }

    @Override public void onAddMediaClicked() { onAdd.run(); }
    @Override public void onMediaClicked(AlbumMedia m, int p) { onClick.accept(m, p); }
    @Override public void onMediaLongPressed(AlbumMedia m, int p) { onLongClick.accept(m, p); }
}
