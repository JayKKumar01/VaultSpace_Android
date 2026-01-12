package com.github.jaykkumar01.vaultspace.views.popups.old.core;

import android.content.Context;

public abstract class ModalSpec {

    public abstract ModalType getType();
    public abstract ModalPriority getPriority();

    public boolean allowReplaceBySamePriority() {
        return false;
    }

    public abstract ModalController createController(Context context);

    public void onShow() {}
    public void onDismiss(ModalDismissReason reason) {}
}
