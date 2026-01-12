package com.github.jaykkumar01.vaultspace.views.popups.list;

import android.content.Context;
import android.view.View;

import com.github.jaykkumar01.vaultspace.views.popups.core.EventModal;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalEnums;

public class ListModal extends EventModal {

    public ListModal(ModalEnums.Priority priority) {
        super(priority);
    }

    /** User selects an item */
    public void onItemSelected(int index) {
        onDismissed(ModalEnums.DismissResult.CONFIRMED, index);
    }

    /** User cancels */
    public void onCancel() {
        onDismissed(ModalEnums.DismissResult.CANCELED, null);
    }

    @Override
    public View createView(Context context) {
        return null;
    }
}
