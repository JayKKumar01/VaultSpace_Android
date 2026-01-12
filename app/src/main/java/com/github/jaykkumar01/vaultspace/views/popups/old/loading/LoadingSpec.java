package com.github.jaykkumar01.vaultspace.views.popups.old.loading;

import android.content.Context;

import com.github.jaykkumar01.vaultspace.views.popups.old.core.ModalController;
import com.github.jaykkumar01.vaultspace.views.popups.old.core.ModalPriority;
import com.github.jaykkumar01.vaultspace.views.popups.old.core.ModalSpec;
import com.github.jaykkumar01.vaultspace.views.popups.old.core.ModalType;

public final class LoadingSpec extends ModalSpec {

    @Override
    public ModalType getType() {
        return ModalType.STATE;
    }

    @Override
    public ModalPriority getPriority() {
        return ModalPriority.PASSIVE;
    }

    @Override
    public ModalController createController(Context context) {
        return new LoadingController(context);
    }
}
