package com.github.jaykkumar01.vaultspace.views.creative.delete;

import android.view.View;

public final class DeleteStatusRenderModel {
    public final CharSequence title;
    public final float progress;
    public final View.OnClickListener onCancel;

    public DeleteStatusRenderModel(
            CharSequence title,
            float progress,
            View.OnClickListener onCancel
    ) {
        this.title = title;
        this.progress = progress;
        this.onCancel = onCancel;
    }
}
