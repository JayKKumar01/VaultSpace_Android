package com.github.jaykkumar01.vaultspace.views.popups.old.confirm;

import android.content.Context;

import com.github.jaykkumar01.vaultspace.views.popups.core.*;
import com.github.jaykkumar01.vaultspace.views.popups.old.core.ModalController;
import com.github.jaykkumar01.vaultspace.views.popups.old.core.ModalPriority;
import com.github.jaykkumar01.vaultspace.views.popups.old.core.ModalSpec;
import com.github.jaykkumar01.vaultspace.views.popups.old.core.ModalType;

public final class ConfirmSpec extends ModalSpec {

    public final String title;
    public final String message;
    public final String positiveText;
    public final boolean showCancel;

    public final boolean dismissOnOutside;
    public final boolean dismissOnBack;

    public final ConfirmRisk risk;

    public final Runnable onConfirm;
    public final Runnable onCancel;

    public ConfirmSpec(
            String title,
            String message,
            String positiveText,
            boolean showCancel,
            boolean dismissOnOutside,
            boolean dismissOnBack,
            ConfirmRisk risk,
            Runnable onConfirm,
            Runnable onCancel
    ) {
        this.title = title;
        this.message = message;
        this.positiveText = positiveText;
        this.showCancel = showCancel;
        this.dismissOnOutside = dismissOnOutside;
        this.dismissOnBack = dismissOnBack;
        this.risk = risk;
        this.onConfirm = onConfirm;
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
        return new ConfirmController(context, this);
    }
}
