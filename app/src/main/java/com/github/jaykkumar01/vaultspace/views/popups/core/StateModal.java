package com.github.jaykkumar01.vaultspace.views.popups.core;

public abstract class StateModal extends Modal {

    protected StateModal() {
        super(ModalEnums.Kind.STATE);
    }

    /** Unique identity for deduplication */
    public abstract String getStateKey();

    /** States cannot be dismissed by user */
    @Override
    public boolean canDismiss(ModalEnums.DismissRequest request) {
        return false;
    }

    /** States are singleton by default */
    @Override
    public boolean allowMultipleInstances() {
        return false;
    }
}
