package com.github.jaykkumar01.vaultspace.album.listener;

import com.github.jaykkumar01.vaultspace.core.upload.base.UploadObserver;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSnapshot;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadedItem;

import java.util.function.Consumer;

public final class UploadObserverImpl implements UploadObserver {

    private final Consumer<UploadSnapshot> onSnapshot;
    private final Runnable onCancelled;
    private final Consumer<UploadedItem> onSuccess;
    private final Consumer<UploadSelection> onFailure;
    private final TriConsumer<UploadSelection, Long, Long> onProgress;

    public UploadObserverImpl(
            Consumer<UploadSnapshot> onSnapshot,
            Runnable onCancelled,
            Consumer<UploadedItem> onSuccess,
            Consumer<UploadSelection> onFailure,
            TriConsumer<UploadSelection, Long, Long> onProgress
    ) {
        this.onSnapshot = onSnapshot;
        this.onCancelled = onCancelled;
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
        this.onProgress = onProgress;
    }

    @Override public void onSnapshot(UploadSnapshot s) { onSnapshot.accept(s); }
    @Override public void onCancelled() { onCancelled.run(); }
    @Override public void onSuccess(UploadedItem i) { onSuccess.accept(i); }
    @Override public void onFailure(UploadSelection s) { onFailure.accept(s); }

    @Override
    public void onProgress(UploadSelection selection, long uploaded, long total) {
        onProgress.accept(selection, uploaded, total);
    }
}
