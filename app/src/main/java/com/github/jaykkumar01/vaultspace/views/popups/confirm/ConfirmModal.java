package com.github.jaykkumar01.vaultspace.views.popups.confirm;

import android.content.Context;
import android.view.View;

import com.github.jaykkumar01.vaultspace.views.popups.core.EventModal;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalEnums;

public class ConfirmModal extends EventModal {

    private final ConfirmSpec spec;

    public ConfirmModal(ConfirmSpec spec) {
        super(ModalEnums.Priority.HIGH);
        this.spec = spec;
    }

    @Override
    public View createView(Context context) {
        return new ConfirmView(
                context,
                spec.title,
                spec.message,
                spec.showNegative,
                spec.riskLevel,
                () -> {
                    requestDismiss(ModalEnums.DismissResult.CONFIRMED, null);
                    if (spec.onPositive != null) spec.onPositive.run();
                },
                () -> {
                    requestDismiss(ModalEnums.DismissResult.CANCELED, null);
                    if (spec.onNegative != null) spec.onNegative.run();
                },
                spec.positiveText,
                spec.negativeText
        );
    }

    @Override
    public boolean canDismiss(ModalEnums.DismissRequest request) {
        return spec.showNegative && spec.riskLevel != ConfirmView.RISK_CRITICAL && spec.cancelable;
    }
}
