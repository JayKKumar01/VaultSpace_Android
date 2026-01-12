package com.github.jaykkumar01.vaultspace.views.popups.confirm;

import android.content.Context;
import android.view.View;

import com.github.jaykkumar01.vaultspace.views.popups.core.EventModal;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalEnums;

public class ConfirmModal extends EventModal {

    private final ConfirmSpec spec;

    public ConfirmModal(ConfirmSpec spec) {
        super(spec.priority);
        this.spec = spec;
    }

    @Override
    public View createView(Context context) {
        return new ConfirmView(
                context,
                spec.title,
                spec.message,
                spec.allowNegative,
                () -> {
                    onDismissed(ModalEnums.DismissResult.CONFIRMED, null);
                    spec.onPositive.run();
                },
                () -> {
                    onDismissed(ModalEnums.DismissResult.CANCELED, null);
                    if (spec.onNegative != null) spec.onNegative.run();

                }
        );
    }

    @Override
    public boolean canDismiss(ModalEnums.DismissRequest request) {
        return spec.allowNegative;
    }
}
