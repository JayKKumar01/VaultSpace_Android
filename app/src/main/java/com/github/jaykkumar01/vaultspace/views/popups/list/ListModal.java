package com.github.jaykkumar01.vaultspace.views.popups.list;

import android.content.Context;
import android.view.View;

import com.github.jaykkumar01.vaultspace.views.popups.core.EventModal;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalEnums;

public class ListModal extends EventModal {

    private final ListSpec spec;

    public ListModal(ListSpec spec) {
        super(ModalEnums.Priority.MEDIUM);
        this.spec = spec;
    }

    @Override
    public View createView(Context context) {
        return new ListView(
                context,
                spec.title,
                spec.items,
                index -> requestDismiss(ModalEnums.DismissResult.CONFIRMED, index)
        );
    }

    @Override
    public void onDismissed(ModalEnums.DismissResult result, Object data) {
        if (result == ModalEnums.DismissResult.CONFIRMED) {
            if (spec.onItemSelected != null && data instanceof Integer) {
                spec.onItemSelected.accept((Integer) data);
            }
        } else if (result == ModalEnums.DismissResult.CANCELED) {
            if (spec.onCanceled != null) {
                spec.onCanceled.run();
            }
        }
    }
}
