package com.github.jaykkumar01.vaultspace.views.popups.form;

import com.github.jaykkumar01.vaultspace.views.popups.core.Modal;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalSpec;

import java.util.function.Consumer;

public class FormSpec extends ModalSpec {

    public final String title;
    public final String hint;
    public final String positiveText;

    public Consumer<String> onSubmit;
    public Runnable onCanceled;

    public FormSpec(
            String title,
            String hint,
            String positiveText,
            Consumer<String> onSubmit,
            Runnable onCanceled
    ) {
        this.title = title;
        this.hint = hint;
        this.positiveText = positiveText;
        this.onSubmit = onSubmit;
        this.onCanceled = onCanceled;
    }

    @Override
    public Modal createModal() {
        return new FormModal(this);
    }
}
