package com.github.jaykkumar01.vaultspace.views.popups.old.core;

import android.view.View;

public interface ModalController {

    View getView();

    boolean dismissOnOutsideTouch();
    boolean dismissOnBackPress();

    void onConfirm();
    void onCancel();

    void onShow();
    void onDismiss(ModalDismissReason reason);

    /* Host injects this */
    void attachDismissRequester(DismissRequester requester);

    interface DismissRequester {
        void dismiss(ModalDismissReason reason);
    }
}
