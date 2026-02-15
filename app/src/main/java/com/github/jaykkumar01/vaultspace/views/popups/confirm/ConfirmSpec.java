package com.github.jaykkumar01.vaultspace.views.popups.confirm;

import com.github.jaykkumar01.vaultspace.views.popups.core.Modal;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalSpec;

public class ConfirmSpec extends ModalSpec {

    public final String title;
    public final String message;
    public final boolean showNegative;
    public final int riskLevel;

    public Runnable onPositive;
    public Runnable onNegative;

    // Optional button labels
    public String positiveText;
    public String negativeText;

    // NEW: modal cancelability (back / outside)
    public boolean cancelable = true;

    public ConfirmSpec(
            String title,
            String message,
            boolean showNegative,
            int riskLevel,
            Runnable onPositive,
            Runnable onNegative
    ) {
        this.title = title;
        this.message = message;
        this.showNegative = showNegative;
        this.riskLevel = riskLevel;
        this.onPositive = onPositive;
        this.onNegative = onNegative;
    }

    @Override
    public Modal createModal() {
        return new ConfirmModal(this);
    }

    /* =======================
       Action setters
       ======================= */

    public void setPositiveAction(Runnable onPositive) {
        this.onPositive = onPositive;
    }

    public void setNegativeAction(Runnable onNegative) {
        this.onNegative = onNegative;
    }

    /* =======================
       Label setters
       ======================= */

    public void setPositiveText(String text) {
        this.positiveText = text;
    }

    public void setNegativeText(String text) {
        this.negativeText = text;
    }

    /* =======================
       Cancelability
       ======================= */

    public void setCancelable(boolean cancelable) {
        this.cancelable = cancelable;
    }
}
