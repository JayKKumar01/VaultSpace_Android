package com.github.jaykkumar01.vaultspace.views.popups.form;

import android.content.Context;
import android.view.View;

import com.github.jaykkumar01.vaultspace.views.popups.core.EventModal;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalEnums;

public class FormModal extends EventModal {

    private final boolean allowCancel;

    public FormModal(ModalEnums.Priority priority, boolean allowCancel) {
        super(priority);
        this.allowCancel = allowCancel;
    }

    /** User submits the form */
    public void onSubmit(Object formData) {
        onDismissed(ModalEnums.DismissResult.CONFIRMED, formData);
    }

    /** User cancels the form */
    public void onCancel() {
        if (!allowCancel) return;
        onDismissed(ModalEnums.DismissResult.CANCELED, null);
    }

    @Override
    public View createView(Context context) {
        return null;
    }

    @Override
    public boolean canDismiss(ModalEnums.DismissRequest request) {
        return allowCancel;
    }
}
