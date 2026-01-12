package com.github.jaykkumar01.vaultspace.views.popups.old.form;

import android.content.Context;
import android.view.View;

import com.github.jaykkumar01.vaultspace.views.popups.FolderActionView;
import com.github.jaykkumar01.vaultspace.views.popups.old.core.ModalController;
import com.github.jaykkumar01.vaultspace.views.popups.old.core.ModalDismissReason;

final class FormController implements ModalController {

    private final FormSpec spec;
    private final FolderActionView view;

    FormController(Context context, FormSpec spec) {
        this.spec = spec;
        this.view = new FolderActionView(context);

        this.view.show(
                spec.title,
                spec.hint,
                spec.positiveText,
                "FormSpec",
                new FolderActionView.Callback() {
                    @Override
                    public void onCreate(String name) {
                        if (spec.onSubmit != null) {
                            spec.onSubmit.submit(name);
                        }
                    }

                    @Override
                    public void onCancel() {
                        if (spec.onCancel != null) {
                            spec.onCancel.run();
                        }
                    }
                }
        );
    }

    @Override
    public View getView() {
        return view;
    }

    @Override
    public boolean dismissOnOutsideTouch() {
        return spec.dismissOnOutside;
    }

    @Override
    public boolean dismissOnBackPress() {
        return spec.dismissOnBack;
    }

    @Override
    public void onConfirm() {
        // handled internally by FolderActionView
    }

    @Override
    public void onCancel() {
        view.hide();
        if (spec.onCancel != null) {
            spec.onCancel.run();
        }
    }

    @Override
    public void onShow() { }

    @Override
    public void onDismiss(ModalDismissReason reason) {
        view.hide();
    }

    @Override
    public void attachDismissRequester(DismissRequester requester) {

    }
}
