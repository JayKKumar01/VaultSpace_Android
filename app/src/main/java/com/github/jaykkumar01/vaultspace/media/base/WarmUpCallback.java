package com.github.jaykkumar01.vaultspace.media.base;

import androidx.annotation.NonNull;

public interface WarmUpCallback {
    void onSuccess();
    void onFailure(@NonNull Exception e);
}
