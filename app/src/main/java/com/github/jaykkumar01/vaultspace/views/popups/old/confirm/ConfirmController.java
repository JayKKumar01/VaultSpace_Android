package com.github.jaykkumar01.vaultspace.views.popups.old.confirm;

import android.content.Context;
import android.view.View;

import com.github.jaykkumar01.vaultspace.views.popups.old.core.ModalController;
import com.github.jaykkumar01.vaultspace.views.popups.old.core.ModalDismissReason;

final class ConfirmController implements ModalController {

    private final ConfirmSpec spec;
    private final ConfirmView view;

    private DismissRequester dismissRequester;

    ConfirmController(Context context, ConfirmSpec spec) {
        this.spec = spec;
        this.view = new ConfirmView(
                context,
                spec.title,
                spec.message,
                spec.positiveText,
                spec.showCancel,
                spec.risk,
                this::handleConfirm,
                this::handleCancel
        );
    }

    @Override public View getView() { return view; }
    @Override public boolean dismissOnOutsideTouch() { return spec.dismissOnOutside; }
    @Override public boolean dismissOnBackPress() { return spec.dismissOnBack; }

    @Override
    public void attachDismissRequester(DismissRequester requester) {
        this.dismissRequester = requester;
    }

    @Override public void onConfirm() { handleConfirm(); }
    @Override public void onCancel() { handleCancel(); }

    private void handleConfirm() {
        if (spec.onConfirm != null) spec.onConfirm.run();
        dismissRequester.dismiss(ModalDismissReason.CONFIRMED);
    }

    private void handleCancel() {
        if (spec.onCancel != null) spec.onCancel.run();
        dismissRequester.dismiss(ModalDismissReason.CANCELED);
    }

    @Override public void onShow() {}
    @Override public void onDismiss(ModalDismissReason reason) {}



}
