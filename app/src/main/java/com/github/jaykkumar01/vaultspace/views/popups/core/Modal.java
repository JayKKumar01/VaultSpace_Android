package com.github.jaykkumar01.vaultspace.views.popups.core;

import android.content.Context;
import android.view.View;

public abstract class Modal {

    private final ModalEnums.Kind kind;

    protected Modal(ModalEnums.Kind kind) {
        this.kind = kind;
    }

    public final ModalEnums.Kind getKind() {
        return kind;
    }

    /** Create the root view for this modal */
    public abstract View createView(Context context);

    public void onShow() {}
    public void onHide() {}

    public boolean canDismiss(ModalEnums.DismissRequest request) {
        return true;
    }

    public boolean allowMultipleInstances() {
        return true;
    }

    public void onDismissed(ModalEnums.DismissResult result, Object data) {}
}
