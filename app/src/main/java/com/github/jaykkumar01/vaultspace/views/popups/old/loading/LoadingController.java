package com.github.jaykkumar01.vaultspace.views.popups.old.loading;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.views.popups.old.core.ModalController;
import com.github.jaykkumar01.vaultspace.views.popups.old.core.ModalDismissReason;

final class LoadingController implements ModalController {

    private final View view;

    LoadingController(Context context) {
        this.view = createView(context);
    }

    private View createView(Context context) {
        FrameLayout root = new FrameLayout(context);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        root.setBackgroundColor(0x990D1117);
        root.setClickable(true);

        ProgressBar progressBar = new ProgressBar(context);
        progressBar.setIndeterminateTintList(
                ColorStateList.valueOf(
                        context.getColor(R.color.vs_accent_primary)
                )
        );

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.gravity = Gravity.CENTER;

        root.addView(progressBar, lp);
        return root;
    }

    @Override
    public View getView() {
        return view;
    }

    @Override
    public boolean dismissOnOutsideTouch() {
        return false;
    }

    @Override
    public boolean dismissOnBackPress() {
        return false;
    }

    @Override
    public void onConfirm() {
        // no-op
    }

    @Override
    public void onCancel() {
        // no-op
    }

    @Override
    public void onShow() { }

    @Override
    public void onDismiss(ModalDismissReason reason) { }

    @Override
    public void attachDismissRequester(DismissRequester requester) {

    }
}
