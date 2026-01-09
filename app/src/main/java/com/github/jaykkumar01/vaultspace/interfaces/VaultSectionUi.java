package com.github.jaykkumar01.vaultspace.interfaces;

public interface VaultSectionUi {
    void show();
    boolean onBackPressed();
    default void release() {}
}