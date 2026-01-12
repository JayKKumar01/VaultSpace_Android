package com.github.jaykkumar01.vaultspace.views.popups.old.list;

import android.content.Context;

import com.github.jaykkumar01.vaultspace.views.popups.core.*;
import com.github.jaykkumar01.vaultspace.views.popups.old.core.ModalController;
import com.github.jaykkumar01.vaultspace.views.popups.old.core.ModalPriority;
import com.github.jaykkumar01.vaultspace.views.popups.old.core.ModalSpec;
import com.github.jaykkumar01.vaultspace.views.popups.old.core.ModalType;

public final class ListActionSpec extends ModalSpec {

    public final String title;
    public final String[] actions;

    public final boolean dismissOnOutside;
    public final boolean dismissOnBack;

    public final OnActionSelected onActionSelected;
    public final Runnable onCancel;

    public interface OnActionSelected {
        void onSelect(int index, String label);
    }

    public ListActionSpec(
            String title,
            String[] actions,
            boolean dismissOnOutside,
            boolean dismissOnBack,
            OnActionSelected onActionSelected,
            Runnable onCancel
    ) {
        this.title = title;
        this.actions = actions;
        this.dismissOnOutside = dismissOnOutside;
        this.dismissOnBack = dismissOnBack;
        this.onActionSelected = onActionSelected;
        this.onCancel = onCancel;
    }

    @Override
    public ModalType getType() {
        return ModalType.EVENT;
    }

    @Override
    public ModalPriority getPriority() {
        return ModalPriority.ACTION;
    }

    @Override
    public ModalController createController(Context context) {
        return new ListActionController(context, this);
    }
}
