package com.github.jaykkumar01.vaultspace.core.upload.base;

public interface ProgressCallback{
    void onProgress(long uploaded,long total);
}
