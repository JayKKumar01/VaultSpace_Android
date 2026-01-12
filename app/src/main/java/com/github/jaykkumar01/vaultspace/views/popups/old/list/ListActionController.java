package com.github.jaykkumar01.vaultspace.views.popups.old.list;

import android.content.Context;
import android.view.View;

import com.github.jaykkumar01.vaultspace.views.popups.ItemActionView;
import com.github.jaykkumar01.vaultspace.views.popups.old.core.ModalController;
import com.github.jaykkumar01.vaultspace.views.popups.old.core.ModalDismissReason;

final class ListActionController implements ModalController {

    private final ListActionSpec spec;
    private final ItemActionView view;

    ListActionController(Context context, ListActionSpec spec) {
        this.spec = spec;
        this.view = new ItemActionView(context);

        this.view.show(
                spec.title,
                spec.actions,
                "ListActionSpec",
                new ItemActionView.Callback() {
                    @Override
                    public void onActionSelected(int index, String label) {
                        if (spec.onActionSelected != null) {
                            spec.onActionSelected.onSelect(index, label);
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
        // Not used for list actions
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
