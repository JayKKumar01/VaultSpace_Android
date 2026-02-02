package com.github.jaykkumar01.vaultspace.views.creative;

import android.graphics.Outline;
import android.view.View;
import android.view.ViewOutlineProvider;

public final class RoundedOutline extends ViewOutlineProvider {

    private final float radius;

    public RoundedOutline(float radius) {
        this.radius = radius;
    }

    @Override
    public void getOutline(View view, Outline outline) {
        outline.setRoundRect(
                0, 0,
                view.getWidth(),
                view.getHeight(),
                radius
        );
    }
}
