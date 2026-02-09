package com.github.jaykkumar01.vaultspace.media.base;

import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSpec;
@UnstableApi
public interface RangeDecisionCallback {
    void onInit(String fileId, DataSpec spec);
    void onStart(String fileId, DataSpec spec, long initToStartMs);
    void onEnd(String fileId, DataSpec spec, int requestCount, int resetCount);
}
