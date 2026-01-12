package com.github.jaykkumar01.vaultspace.views.popups.core;

public abstract class EventModal extends Modal {

    private final ModalEnums.Priority priority;

    protected EventModal(ModalEnums.Priority priority) {
        super(ModalEnums.Kind.EVENT);
        this.priority = priority;
    }

    public final ModalEnums.Priority getPriority() {
        return priority;
    }

    // allowMultipleInstances() inherited → true
    // canDismiss() inherited → true (override if needed)
}
