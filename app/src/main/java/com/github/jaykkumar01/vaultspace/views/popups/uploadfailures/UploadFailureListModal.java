package com.github.jaykkumar01.vaultspace.views.popups.uploadfailures;

import android.content.Context;
import android.view.View;

import com.github.jaykkumar01.vaultspace.views.popups.core.EventModal;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalEnums;

public final class UploadFailureListModal extends EventModal {

    private final UploadFailureListSpec spec;

    public UploadFailureListModal(UploadFailureListSpec spec) {
        super(ModalEnums.Priority.MEDIUM);
        this.spec = spec;
    }

    @Override
    public View createView(Context context) {
        return new UploadFailureListView(
                context,
                spec.title,
                spec.failures,
                () -> requestDismiss(ModalEnums.DismissResult.CONFIRMED, null)
        );
    }

    @Override
    public boolean canDismiss(ModalEnums.DismissRequest request) {
        return false; // OK only
    }

    @Override
    public void onDismissed(ModalEnums.DismissResult result, Object data) {
        if (spec.onOk != null) spec.onOk.run();
    }
}
