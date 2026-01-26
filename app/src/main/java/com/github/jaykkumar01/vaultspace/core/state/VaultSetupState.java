package com.github.jaykkumar01.vaultspace.core.state;

import java.util.concurrent.atomic.AtomicBoolean;

public final class VaultSetupState {

    private static volatile VaultSetupState INSTANCE;

    // Whether setup is required right now
    private final AtomicBoolean setupRequired = new AtomicBoolean(false);

    private VaultSetupState() {}

    public static VaultSetupState get() {
        if (INSTANCE == null) {
            synchronized (VaultSetupState.class) {
                if (INSTANCE == null) {
                    INSTANCE = new VaultSetupState();
                }
            }
        }
        return INSTANCE;
    }

    /* ==========================================================
     * Public API
     * ========================================================== */

    /** Called when app detects no usable trusted accounts */
    public void markSetupRequired() {
        setupRequired.set(true);
    }

    /** Called when user finishes setup successfully */
    public void markSetupComplete() {
        setupRequired.set(false);
    }

    /** Read-only query for UI / workers */
    public boolean isSetupRequired() {
        return setupRequired.get();
    }
}
