package com.github.jaykkumar01.vaultspace.views.popups.old.core;

public enum ModalPriority {
    CRITICAL(4),
    FORM(3),
    ACTION(2),
    PASSIVE(1);

    public final int priority;

    ModalPriority(int priority) {
        this.priority = priority;
    }
}
