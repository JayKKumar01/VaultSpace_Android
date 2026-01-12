package com.github.jaykkumar01.vaultspace.views.popups.confirm;

import com.github.jaykkumar01.vaultspace.views.popups.core.Modal;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalEnums;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalSpec;

public class ConfirmSpec extends ModalSpec {

    public final String title;
    public final String message;
    public final boolean allowNegative;
    public final Runnable onPositive;
    public final Runnable onNegative;
    public final ModalEnums.Priority priority;

    public ConfirmSpec(
            String title,
            String message,
            boolean allowNegative,
            ModalEnums.Priority priority,
            Runnable onPositive,
            Runnable onNegative
    ) {
        this.title = title;
        this.message = message;
        this.allowNegative = allowNegative;
        this.priority = priority;
        this.onPositive = onPositive;
        this.onNegative = onNegative;
    }

    @Override
    public Modal createModal() {
        return new ConfirmModal(this);
    }
}
