package com.github.jaykkumar01.vaultspace.views.popups.loading;

import android.content.Context;
import android.view.View;

import com.github.jaykkumar01.vaultspace.views.popups.core.StateModal;

public class LoadingModal extends StateModal {

    @Override
    public String getStateKey() {
        return "LOADING";
    }

    @Override
    public View createView(Context context) {
        return new LoadingModalView(context);
    }
}
