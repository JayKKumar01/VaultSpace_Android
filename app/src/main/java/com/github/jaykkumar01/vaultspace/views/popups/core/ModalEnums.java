package com.github.jaykkumar01.vaultspace.views.popups.core;

public final class ModalEnums {

    private ModalEnums() {
        // no instances
    }

    /** What kind of modal this is */
    public enum Kind {
        STATE,   // loading, login, syncing
        EVENT    // confirm, form, list
    }

    /** Priority (used only by EVENT modals) */
    public enum Priority {
        LOW(1),
        MEDIUM(2),
        HIGH(3),
        CRITICAL(4);

        private final int value;

        Priority(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    /** Why dismissal is being requested */
    public enum DismissRequest {
        BACK_PRESS,
        OUTSIDE_TOUCH,
        USER_ACTION,
        SYSTEM
    }

    /** How the modal actually ended */
    public enum DismissResult {
        CONFIRMED,
        CANCELED,
        REPLACED,
        SYSTEM
    }

    /** What the modal decides when dismissal is requested */
    public enum DismissDecision {
        ALLOW,     // dismiss now
        BLOCK,     // ignore request
        REDIRECT   // show another modal instead
    }
}
