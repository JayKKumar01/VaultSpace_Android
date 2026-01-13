package com.github.jaykkumar01.vaultspace.views.popups.form;

import android.content.Context;
import android.view.View;

import com.github.jaykkumar01.vaultspace.views.popups.core.EventModal;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalEnums;

public class FormModal extends EventModal {

    private final FormSpec spec;

    public FormModal(FormSpec spec) {
        super(ModalEnums.Priority.MEDIUM);
        this.spec = spec;
    }

    @Override
    public View createView(Context context) {
        return new FormView(
                context,
                spec.title,
                spec.hint,
                spec.positiveText,
                value -> requestDismiss(
                        ModalEnums.DismissResult.CONFIRMED,
                        value
                ),
                () -> requestDismiss(
                        ModalEnums.DismissResult.CANCELED,
                        null
                )
        );
    }

    @Override
    public void onDismissed(ModalEnums.DismissResult result, Object data) {
        if (result == ModalEnums.DismissResult.CONFIRMED) {
            if (spec.onSubmit != null && data instanceof String) {
                spec.onSubmit.accept((String) data);
            }
        } else if (result == ModalEnums.DismissResult.CANCELED) {
            if (spec.onCanceled != null) {
                spec.onCanceled.run();
            }
        }
    }
}
