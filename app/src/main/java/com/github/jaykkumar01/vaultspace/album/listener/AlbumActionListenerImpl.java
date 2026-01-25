package com.github.jaykkumar01.vaultspace.album.listener;

import com.github.jaykkumar01.vaultspace.album.coordinator.AlbumActionCoordinator;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSelection;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public final class AlbumActionListenerImpl implements AlbumActionCoordinator.Listener {

    private final IntConsumer onSelected;
    private final Consumer<List<UploadSelection>> onResolved;

    public AlbumActionListenerImpl(
            IntConsumer onSelected,
            Consumer<List<UploadSelection>> onResolved
    ) {
        this.onSelected = onSelected;
        this.onResolved = onResolved;
    }

    @Override public void onMediaSelected(int size) { onSelected.accept(size); }
    @Override public void onMediaResolved(List<UploadSelection> s) { onResolved.accept(s); }
}
