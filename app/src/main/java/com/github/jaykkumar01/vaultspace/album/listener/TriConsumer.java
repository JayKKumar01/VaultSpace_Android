package com.github.jaykkumar01.vaultspace.album.listener;

@FunctionalInterface
public interface TriConsumer<A, B, C> {
    void accept(A a, B b, C c);
}
