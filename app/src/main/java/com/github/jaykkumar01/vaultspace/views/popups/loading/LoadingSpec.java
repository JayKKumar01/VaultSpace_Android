package com.github.jaykkumar01.vaultspace.views.popups.loading;


import com.github.jaykkumar01.vaultspace.views.popups.core.Modal;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalSpec;

public class LoadingSpec extends ModalSpec {

    @Override
    public Modal createModal() {
        return new LoadingModal();
    }
}
