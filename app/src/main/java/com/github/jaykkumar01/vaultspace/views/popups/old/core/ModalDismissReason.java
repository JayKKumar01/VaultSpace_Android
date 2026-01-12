package com.github.jaykkumar01.vaultspace.views.popups.old.core;

public enum ModalDismissReason {
    CONFIRMED,   // user completed the action
    CANCELED,    // user backed out
    SYSTEM,      // app logic cleared it
    REPLACED     // interrupted by higher/same priority modal
}
