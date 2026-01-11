package com.github.jaykkumar01.vaultspace.core.session.cache;

/**
 * Base class for all session-scoped caches.
 *
 * Responsibilities:
 * - Manage cache lifecycle only
 * - Enforce explicit activation and clearing
 *
 * Non-responsibilities:
 * - Store data
 * - Expose collections
 * - Provide domain APIs
 * - Guarantee performance (subclasses must)
 */
public abstract class VaultCache {

    /* ==========================================================
     * Cache Lifecycle State
     * ========================================================== */

    public enum State {
        UNINITIALIZED,
        INITIALIZED,
        CLEARED
    }

    private State state = State.UNINITIALIZED;

    /* ==========================================================
     * Public lifecycle inspection
     * ========================================================== */

    public final State getState() {
        return state;
    }

    public final boolean isInitialized() {
        return state == State.INITIALIZED;
    }

    /* ==========================================================
     * Public lifecycle control
     * ========================================================== */

    /**
     * Clears this cache completely and marks it as CLEARED.
     * Called on logout or session reset.
     *
     * This method is FINAL to prevent subclasses
     * from breaking lifecycle guarantees.
     */
    public final void clear() {
        if (state != State.CLEARED) {
            onClear();
            state = State.CLEARED;
        }
    }

    /* ==========================================================
     * Protected lifecycle control (subclasses only)
     * ========================================================== */

    /**
     * Marks this cache as initialized.
     * Subclasses decide WHEN initialization is valid.
     */
    protected final void markInitialized() {
        state = State.INITIALIZED;
    }

    /**
     * Resets lifecycle state to UNINITIALIZED.
     * Rarely needed; intended for advanced flows only.
     */
    protected final void resetState() {
        state = State.UNINITIALIZED;
    }

    /* ==========================================================
     * Hooks for subclasses
     * ========================================================== */

    /**
     * Subclasses must clear all internal data here.
     * This method is called exactly once per clear().
     */
    protected abstract void onClear();
}
