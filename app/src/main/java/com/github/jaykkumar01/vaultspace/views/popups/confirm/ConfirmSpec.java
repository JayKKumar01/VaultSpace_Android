package com.github.jaykkumar01.vaultspace.views.popups.confirm;

import com.github.jaykkumar01.vaultspace.views.popups.core.Modal;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalSpec;
public final class ConfirmSpec extends ModalSpec {

    public final String title;
    public final String message;
    public final int riskLevel;
    public boolean cancelable = true;
    public String positiveText;
    public String negativeText;
    public Runnable onPositive;
    public Runnable onNegative;

    public ConfirmSpec(String title, String message, int riskLevel) {
        this.title = title;
        this.message = message;
        this.riskLevel = riskLevel;
    }

    @Override
    public Modal createModal() {
        return new ConfirmModal(this);
    }

    /* Actions */

    public void onPositive(Runnable action) {
        this.onPositive = action;
    }

    public void onNegative(Runnable action) {
        this.onNegative = action;
    }

    /* Labels */

    public void setPositiveText(String text) {
        this.positiveText = text;
    }

    public void setNegativeText(String text) {
        this.negativeText = text;
    }

    public void setCancelable(boolean value) {
        this.cancelable = value;
    }
}
