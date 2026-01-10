package com.github.jaykkumar01.vaultspace.models;

public final class VaultStorageState {

    public final float used;
    public final float total;
    public final String unit;

    public VaultStorageState(float used, float total, String unit) {
        this.used = used;
        this.total = total;
        this.unit = unit;
    }
}
