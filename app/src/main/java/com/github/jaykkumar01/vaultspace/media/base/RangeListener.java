package com.github.jaykkumar01.vaultspace.media.base;

interface RangeListener {
    void onRangeRequested(long offset);
    void onClientDisconnected();
}
