package com.github.jaykkumar01.vaultspace.album.listener;

import com.github.jaykkumar01.vaultspace.core.upload.controller.UploadStatusController;

import java.util.function.Consumer;

public final class UploadStatusCallbackImpl implements UploadStatusController.Callback {

    private final Runnable onCancel;
    private final Runnable onRetry;
    private final Runnable onAcknowledge;
    private final Runnable onNoAccess;

    public UploadStatusCallbackImpl(
            Runnable onCancel,
            Runnable onRetry,
            Runnable onAcknowledge,
            Runnable onNoAccess
    ) {
        this.onCancel = onCancel;
        this.onRetry = onRetry;
        this.onAcknowledge = onAcknowledge;
        this.onNoAccess = onNoAccess;
    }

    @Override public void onCancelRequested() { onCancel.run(); }
    @Override public void onRetryRequested() { onRetry.run(); }
    @Override public void onAcknowledge() { onAcknowledge.run(); }
    @Override public void onNoAccessInfo() { onNoAccess.run(); }
}
