package com.github.jaykkumar01.vaultspace.views.popups.old.form;

import android.content.Context;

import com.github.jaykkumar01.vaultspace.views.popups.core.*;
import com.github.jaykkumar01.vaultspace.views.popups.old.core.ModalController;
import com.github.jaykkumar01.vaultspace.views.popups.old.core.ModalPriority;
import com.github.jaykkumar01.vaultspace.views.popups.old.core.ModalSpec;
import com.github.jaykkumar01.vaultspace.views.popups.old.core.ModalType;

public final class FormSpec extends ModalSpec {

    public final String title;
    public final String hint;
    public final String positiveText;

    public final boolean dismissOnOutside;
    public final boolean dismissOnBack;

    public final OnSubmit onSubmit;
    public final Runnable onCancel;

    public interface OnSubmit {
        void submit(String value);
    }

    public FormSpec(
            String title,
            String hint,
            String positiveText,
            boolean dismissOnOutside,
            boolean dismissOnBack,
            OnSubmit onSubmit,
            Runnable onCancel
    ) {
        this.title = title;
        this.hint = hint;
        this.positiveText = positiveText;
        this.dismissOnOutside = dismissOnOutside;
        this.dismissOnBack = dismissOnBack;
        this.onSubmit = onSubmit;
        this.onCancel = onCancel;
    }

    @Override
    public ModalType getType() {
        return ModalType.EVENT;
    }

    @Override
    public ModalPriority getPriority() {
        return ModalPriority.FORM;
    }

    @Override
    public ModalController createController(Context context) {
        return new FormController(context, this);
    }
}
