package com.github.jaykkumar01.vaultspace.core.upload;

import com.github.jaykkumar01.vaultspace.models.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.models.base.UploadedItem;

public interface UploadObserver {

    void onSnapshot(UploadSnapshot snapshot);
    void onCancelled();

    void onSuccess(UploadedItem item);
    void onFailure(UploadSelection selection);
}
