package com.github.jaykkumar01.vaultspace.core.session.cache;

public abstract class VaultCache<T> {

    private boolean cached = false;

    /* ================= Public API (FINAL) ================= */

    public final boolean isCached() {
        return cached;
    }

    public final T get() {
        return cached ? getInternal() : getEmpty();
    }

    public final void set(T data) {
        setInternal(data);
        cached = true;
    }

    public final void clear() {
        clearInternal();
        cached = false;
    }

    /* ================= Hooks for subclasses ================= */

    protected abstract T getInternal();
    protected abstract T getEmpty();
    protected abstract void setInternal(T data);
    protected abstract void clearInternal();
}
